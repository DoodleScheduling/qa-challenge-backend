package doodle.qa.com.svcproviderqa.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.entity.Event;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the EventService. These tests verify the integration between the service
 * layer and repository layer, ensuring that event operations correctly persist data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceIntegrationTest {

  @Autowired private EventService eventService;
  @Autowired private CalendarService calendarService;
  @Autowired private EventRepository eventRepository;
  @Autowired private CalendarRepository calendarRepository;

  private UUID calendarId;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    calendarRepository.deleteAll();
    eventRepository.deleteAll();
    
    // Create a test calendar for events
    CalendarDto calendarDto = TestDataFactory.createCalendarDto("Test Calendar", "Test Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    calendarId = createdCalendar.getId();
    now = LocalDateTime.now();
  }

  @AfterEach
  void tearDown() {
    eventRepository.deleteAll();
    calendarRepository.deleteAll();
  }

  @Test
  @DisplayName("Should successfully create an event and persist it in the database")
  void testCreateEvent() {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Integration Test Event", "Integration Test Description", 
        now, now.plusHours(1), "Integration Test Location", calendarId);

    // When
    EventDto createdEvent = eventService.createEvent(eventDto);

    // Then
    assertThat(createdEvent.getId()).isNotNull();
    assertThat(createdEvent.getTitle()).isEqualTo("Integration Test Event");
    assertThat(createdEvent.getDescription()).isEqualTo("Integration Test Description");
    assertThat(createdEvent.getCalendarId()).isEqualTo(calendarId);

    Event savedEvent = eventRepository.findById(createdEvent.getId()).orElseThrow();
    assertThat(savedEvent.getTitle()).isEqualTo("Integration Test Event");
    assertThat(savedEvent.getDescription()).isEqualTo("Integration Test Description");
    assertThat(savedEvent.getCalendar().getId()).isEqualTo(calendarId);
  }

  @Test
  @DisplayName("Should successfully update an event's information")
  void testUpdateEvent() {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Original Event", "Original Description", 
        now, now.plusHours(1), "Original Location", calendarId);
    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    EventDto updateDto = TestDataFactory.createEventDto(
        eventId, "Updated Event", "Updated Description", 
        now.plusHours(2), now.plusHours(3), "Updated Location", calendarId);
    updateDto.setVersion(createdEvent.getVersion()); // Set the version for optimistic locking

    // When
    EventDto updatedEvent = eventService.updateEvent(eventId, updateDto);

    // Then
    assertThat(updatedEvent.getTitle()).isEqualTo("Updated Event");
    assertThat(updatedEvent.getDescription()).isEqualTo("Updated Description");
    assertThat(updatedEvent.getLocation()).isEqualTo("Updated Location");

    Event savedEvent = eventRepository.findById(eventId).orElseThrow();
    assertThat(savedEvent.getTitle()).isEqualTo("Updated Event");
    assertThat(savedEvent.getDescription()).isEqualTo("Updated Description");
    assertThat(savedEvent.getLocation()).isEqualTo("Updated Location");
  }

  @Test
  @DisplayName("Should successfully delete an event from the database")
  void testDeleteEvent() {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Event To Delete", "Delete Description", 
        now, now.plusHours(1), "Delete Location", calendarId);
    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    // When
    eventService.deleteEvent(eventId);

    // Then
    assertThat(eventRepository.findById(eventId)).isEmpty();
  }

  @Test
  @DisplayName("Should successfully retrieve all events")
  void testGetAllEvents() {
    // Given
    int totalEvents = 5;
    for (int i = 0; i < totalEvents; i++) {
      eventService.createEvent(
          TestDataFactory.createEventDto(
              "Event " + i, "Description " + i, 
              now.plusHours(i), now.plusHours(i + 1), 
              "Location " + i, calendarId));
    }

    // When
    List<EventDto> events = eventService.getAllEvents();

    // Then
    assertThat(events).hasSize(totalEvents);
    for (int i = 0; i < totalEvents; i++) {
      final int index = i;
      assertThat(events).anyMatch(e -> e.getTitle().equals("Event " + index));
    }
  }

  @Test
  @DisplayName("Should successfully retrieve an event by ID")
  void testGetEventById() {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Test Event", "Test Description", 
        now, now.plusHours(1), "Test Location", calendarId);
    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    // When
    EventDto retrievedEvent = eventService.getEventById(eventId);

    // Then
    assertThat(retrievedEvent.getId()).isEqualTo(eventId);
    assertThat(retrievedEvent.getTitle()).isEqualTo("Test Event");
    assertThat(retrievedEvent.getDescription()).isEqualTo("Test Description");
    assertThat(retrievedEvent.getCalendarId()).isEqualTo(calendarId);
  }

  @Test
  @DisplayName("Should successfully retrieve events by calendar ID")
  void testGetEventsByCalendarId() {
    // Given
    int totalEvents = 3;
    for (int i = 0; i < totalEvents; i++) {
      eventService.createEvent(
          TestDataFactory.createEventDto(
              "Event " + i, "Description " + i, 
              now.plusHours(i), now.plusHours(i + 1), 
              "Location " + i, calendarId));
    }

    // When
    List<EventDto> events = eventService.getEventsByCalendarId(calendarId);

    // Then
    assertThat(events).hasSize(totalEvents);
    for (int i = 0; i < totalEvents; i++) {
      final int index = i;
      assertThat(events).anyMatch(e -> e.getTitle().equals("Event " + index));
    }
  }

  @Test
  @DisplayName("Should successfully retrieve events by time range")
  void testGetEventsByTimeRange() {
    // Given
    // Create events at different times
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 1", "Description 1", 
        now, now.plusHours(1), "Location 1", calendarId));
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 2", "Description 2", 
        now.plusHours(2), now.plusHours(3), "Location 2", calendarId));
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 3", "Description 3", 
        now.plusHours(4), now.plusHours(5), "Location 3", calendarId));

    // When
    List<EventDto> events = eventService.getEventsByTimeRange(
        now.minusHours(1), now.plusHours(3));

    // Then
    assertThat(events).hasSize(2);
    assertThat(events).anyMatch(e -> e.getTitle().equals("Event 1"));
    assertThat(events).anyMatch(e -> e.getTitle().equals("Event 2"));
    assertThat(events).noneMatch(e -> e.getTitle().equals("Event 3"));
  }

  @Test
  @DisplayName("Should successfully retrieve events by calendar ID and time range")
  void testGetEventsByCalendarIdAndTimeRange() {
    // Given
    // Create a second calendar
    CalendarDto anotherCalendarDto = TestDataFactory.createCalendarDto("Another Calendar", "Another Description");
    CalendarDto anotherCalendar = calendarService.createCalendar(anotherCalendarDto);
    UUID anotherCalendarId = anotherCalendar.getId();
    
    // Create events in different calendars and times
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 1", "Description 1", 
        now, now.plusHours(1), "Location 1", calendarId));
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 2", "Description 2", 
        now.plusHours(2), now.plusHours(3), "Location 2", calendarId));
    eventService.createEvent(TestDataFactory.createEventDto(
        "Event 3", "Description 3", 
        now.plusHours(1), now.plusHours(2), "Location 3", anotherCalendarId));

    // When
    List<EventDto> events = eventService.getEventsByCalendarIdAndTimeRange(
        calendarId, now.minusHours(1), now.plusHours(3));

    // Then
    assertThat(events).hasSize(2);
    assertThat(events).anyMatch(e -> e.getTitle().equals("Event 1"));
    assertThat(events).anyMatch(e -> e.getTitle().equals("Event 2"));
    assertThat(events).noneMatch(e -> e.getTitle().equals("Event 3"));
  }
}