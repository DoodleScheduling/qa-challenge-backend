package doodle.qa.com.svcproviderqa.integration.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.exception.EventNotFoundException;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.repository.EventRepository;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.service.EventService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests focusing on negative paths and error scenarios. These tests verify that the
 * application correctly handles error conditions and edge cases in an integrated environment.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NegativePathIntegrationTest {

  @Autowired private CalendarService calendarService;
  @Autowired private EventService eventService;
  @Autowired private CalendarRepository calendarRepository;
  @Autowired private EventRepository eventRepository;

  @BeforeEach
  void setUp() {
    eventRepository.deleteAll();
    calendarRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    eventRepository.deleteAll();
    calendarRepository.deleteAll();
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when getting a non-existent calendar")
  void testGetNonExistentCalendar() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> calendarService.getCalendarById(nonExistentId))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when updating a non-existent calendar")
  void testUpdateNonExistentCalendar() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    CalendarDto updateDto =
        TestDataFactory.createCalendarDto(nonExistentId, "Updated Calendar", "Updated Description", null);

    // When/Then
    assertThatThrownBy(() -> calendarService.updateCalendar(nonExistentId, updateDto))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when deleting a non-existent calendar")
  void testDeleteNonExistentCalendar() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> calendarService.deleteCalendar(nonExistentId))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when getting a non-existent event")
  void testGetNonExistentEvent() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> eventService.getEventById(nonExistentId))
        .isInstanceOf(EventNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when updating a non-existent event")
  void testUpdateNonExistentEvent() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    UUID calendarId = createTestCalendar().getId();

    EventDto updateDto = TestDataFactory.createEventDto(
        nonExistentId, "Updated Event", "Updated Description", 
        LocalDateTime.now(), LocalDateTime.now().plusHours(1), 
        "Updated Location", calendarId);

    // When/Then
    assertThatThrownBy(() -> eventService.updateEvent(nonExistentId, updateDto))
        .isInstanceOf(EventNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when deleting a non-existent event")
  void testDeleteNonExistentEvent() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> eventService.deleteEvent(nonExistentId))
        .isInstanceOf(EventNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when creating an event with non-existent calendar")
  void testCreateEventWithNonExistentCalendar() {
    // Given
    UUID nonExistentCalendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto = TestDataFactory.createEventDto(
        "Test Event", "Test Description", 
        now, now.plusHours(1), "Test Location", nonExistentCalendarId);

    // When/Then
    assertThatThrownBy(() -> eventService.createEvent(eventDto))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found with id: " + nonExistentCalendarId);
  }

  @Test
  @DisplayName("Should throw DataIntegrityViolationException when creating a calendar with duplicate name")
  void testCreateCalendarWithDuplicateName() {
    // Given
    String name = "Duplicate Calendar";
    CalendarDto firstCalendar = TestDataFactory.createCalendarDto(name, "First Description");
    calendarService.createCalendar(firstCalendar);

    CalendarDto duplicateCalendar = TestDataFactory.createCalendarDto(name, "Duplicate Description");

    // When/Then
    assertThatThrownBy(() -> calendarService.createCalendar(duplicateCalendar))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when updating an event with non-existent calendar")
  void testUpdateEventWithNonExistentCalendar() {
    // Given
    // Create a calendar and an event
    CalendarDto calendarDto = createTestCalendar();
    UUID calendarId = calendarDto.getId();

    LocalDateTime now = LocalDateTime.now();
    EventDto eventDto = TestDataFactory.createEventDto(
        "Original Event", "Original Description", 
        now, now.plusHours(1), "Original Location", calendarId);

    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    // Try to update with non-existent calendar
    UUID nonExistentCalendarId = UUID.randomUUID();
    EventDto updateDto = TestDataFactory.createEventDto(
        eventId, "Updated Event", "Updated Description", 
        now, now.plusHours(1), "Updated Location", nonExistentCalendarId);
    updateDto.setVersion(createdEvent.getVersion()); // Set the version for optimistic locking

    // When/Then
    assertThatThrownBy(() -> eventService.updateEvent(eventId, updateDto))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found with id: " + nonExistentCalendarId);
  }

  @Test
  @DisplayName("Should return empty list when getting events for non-existent calendar")
  void testGetEventsForNonExistentCalendar() {
    // Given
    UUID nonExistentCalendarId = UUID.randomUUID();

    // When
    List<EventDto> events = eventService.getEventsByCalendarId(nonExistentCalendarId);

    // Then
    assertThat(events).hasSize(0);
  }


  // Helper method to create a test calendar
  private CalendarDto createTestCalendar() {
    CalendarDto calendarDto = TestDataFactory.createCalendarDto("Test Calendar", "Test Description");
    return calendarService.createCalendar(calendarDto);
  }
}
