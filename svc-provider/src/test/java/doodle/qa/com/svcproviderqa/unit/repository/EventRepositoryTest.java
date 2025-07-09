package doodle.qa.com.svcproviderqa.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.entity.Event;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.repository.EventRepository;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the EventRepository. These tests verify the custom repository methods and JPA
 * functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

  @Autowired private EventRepository eventRepository;
  @Autowired private CalendarRepository calendarRepository;

  private Calendar calendar;
  private UUID calendarId;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    calendar = TestDataFactory.createCalendar("Test Calendar", "Test Description");
    calendar = calendarRepository.save(calendar);
    calendarId = calendar.getId();
    now = LocalDateTime.now();
  }

  @Test
  @DisplayName("Should find events by calendar when events exist")
  void findByCalendar_WhenEventsExist_ShouldReturnEvents() {
    // Given
    Event event1 =
        TestDataFactory.createEvent(
            "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendar);
    Event event2 =
        TestDataFactory.createEvent(
            "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendar);

    eventRepository.save(event1);
    eventRepository.save(event2);

    // When
    List<Event> events = eventRepository.findByCalendar(calendar);

    // Then
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getTitle()).isEqualTo("Event 1");
    assertThat(events.get(1).getTitle()).isEqualTo("Event 2");
  }

  @Test
  @DisplayName("Should find events by calendar ID when events exist")
  void findByCalendarId_WhenEventsExist_ShouldReturnEvents() {
    // Given
    Event event1 =
        TestDataFactory.createEvent(
            "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendar);
    Event event2 =
        TestDataFactory.createEvent(
            "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendar);

    eventRepository.save(event1);
    eventRepository.save(event2);

    // When
    List<Event> events = eventRepository.findByCalendarId(calendarId);

    // Then
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getTitle()).isEqualTo("Event 1");
    assertThat(events.get(1).getTitle()).isEqualTo("Event 2");
  }

  @Test
  @DisplayName("Should find events by start time between when events exist")
  void findByStartTimeBetween_WhenEventsExist_ShouldReturnEvents() {
    // Given
    Event event1 =
        TestDataFactory.createEvent(
            "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendar);
    Event event2 =
        TestDataFactory.createEvent(
            "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendar);
    Event event3 =
        TestDataFactory.createEvent(
            "Event 3", "Description 3", now.plusHours(4), now.plusHours(5), "Location 3", calendar);

    eventRepository.save(event1);
    eventRepository.save(event2);
    eventRepository.save(event3);

    // When
    List<Event> events =
        eventRepository.findByStartTimeBetween(now.minusHours(1), now.plusHours(3));

    // Then
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getTitle()).isEqualTo("Event 1");
    assertThat(events.get(1).getTitle()).isEqualTo("Event 2");
  }

  @Test
  @DisplayName("Should find events by calendar ID and start time between when events exist")
  void findByCalendarIdAndStartTimeBetween_WhenEventsExist_ShouldReturnEvents() {
    // Given
    Calendar anotherCalendar =
        TestDataFactory.createCalendar("Another Calendar", "Another Description");
    anotherCalendar = calendarRepository.save(anotherCalendar);

    Event event1 =
        TestDataFactory.createEvent(
            "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendar);
    Event event2 =
        TestDataFactory.createEvent(
            "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendar);
    Event event3 =
        TestDataFactory.createEvent(
            "Event 3",
            "Description 3",
            now.plusHours(1),
            now.plusHours(2),
            "Location 3",
            anotherCalendar);

    eventRepository.save(event1);
    eventRepository.save(event2);
    eventRepository.save(event3);

    // When
    List<Event> events =
        eventRepository.findByCalendarIdAndStartTimeBetween(
            calendarId, now.minusHours(1), now.plusHours(3));

    // Then
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getTitle()).isEqualTo("Event 1");
    assertThat(events.get(1).getTitle()).isEqualTo("Event 2");
  }

  @Test
  @DisplayName("Should save event with calendar reference")
  void save_WithCalendar_ShouldPersistCalendarReference() {
    // Given
    Event event =
        TestDataFactory.createEvent(
            "Test Event", "Test Description", now, now.plusHours(1), "Test Location", calendar);

    // When
    Event savedEvent = eventRepository.save(event);
    Event retrievedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();

    // Then
    assertThat(retrievedEvent.getCalendar()).isNotNull();
    assertThat(retrievedEvent.getCalendar().getId()).isEqualTo(calendarId);
  }

  @Test
  @DisplayName("Should update event when saving with existing ID")
  void save_WithExistingId_ShouldUpdateEvent() {
    // Given
    Event event =
        TestDataFactory.createEvent(
            "Original Title",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            calendar);
    Event savedEvent = eventRepository.save(event);
    UUID eventId = savedEvent.getId();

    // When
    savedEvent.setTitle("Updated Title");
    savedEvent.setDescription("Updated Description");
    savedEvent.setLocation("Updated Location");
    eventRepository.save(savedEvent);
    Event retrievedEvent = eventRepository.findById(eventId).orElseThrow();

    // Then
    assertThat(retrievedEvent.getTitle()).isEqualTo("Updated Title");
    assertThat(retrievedEvent.getDescription()).isEqualTo("Updated Description");
    assertThat(retrievedEvent.getLocation()).isEqualTo("Updated Location");
  }

  @Test
  @DisplayName("Should delete event when event exists")
  void delete_WhenEventExists_ShouldRemoveEvent() {
    // Given
    Event event =
        TestDataFactory.createEvent(
            "Event To Delete",
            "Delete Description",
            now,
            now.plusHours(1),
            "Delete Location",
            calendar);
    Event savedEvent = eventRepository.save(event);
    UUID eventId = savedEvent.getId();

    // When
    eventRepository.delete(savedEvent);
    Optional<Event> retrievedEvent = eventRepository.findById(eventId);

    // Then
    assertThat(retrievedEvent).isEmpty();
  }

  @Test
  @DisplayName("Should not delete calendar when event is deleted")
  void delete_WhenEventExists_ShouldNotRemoveCalendar() {
    // Given
    Event event =
        TestDataFactory.createEvent(
            "Event To Delete",
            "Delete Description",
            now,
            now.plusHours(1),
            "Delete Location",
            calendar);
    Event savedEvent = eventRepository.save(event);

    // When
    eventRepository.delete(savedEvent);
    Optional<Calendar> retrievedCalendar = calendarRepository.findById(calendarId);

    // Then
    assertThat(retrievedCalendar).isPresent();
    assertThat(retrievedCalendar.get().getId()).isEqualTo(calendarId);
  }
}
