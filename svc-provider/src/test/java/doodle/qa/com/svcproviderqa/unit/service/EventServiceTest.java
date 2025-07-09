package doodle.qa.com.svcproviderqa.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.entity.Event;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.exception.ConcurrentModificationException;
import doodle.qa.com.svcproviderqa.exception.EventNotFoundException;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.repository.EventRepository;
import doodle.qa.com.svcproviderqa.service.EventService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Unit tests for the EventService. These tests verify the business logic in the service layer using
 * mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private EventRepository eventRepository;
  @Mock private CalendarRepository calendarRepository;

  private EventService eventService;
  private Validator validator;

  @BeforeEach
  void setUp() {
    eventService = new EventService(eventRepository, calendarRepository);
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should return event by ID when event exists")
  void getEventById_WhenEventExists_ShouldReturnEvent() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();
    Event event =
        TestDataFactory.createEvent(
            eventId,
            "Test Event",
            "Test Description",
            now,
            now.plusHours(1),
            "Test Location",
            calendar);
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

    // When
    EventDto result = eventService.getEventById(eventId);

    // Then
    assertThat(result.getId()).isEqualTo(eventId);
    assertThat(result.getTitle()).isEqualTo("Test Event");
    assertThat(result.getDescription()).isEqualTo("Test Description");
    assertThat(result.getStartTime()).isEqualTo(now);
    assertThat(result.getEndTime()).isEqualTo(now.plusHours(1));
    assertThat(result.getLocation()).isEqualTo("Test Location");
    assertThat(result.getCalendarId()).isEqualTo(calendarId);
    verify(eventRepository).findById(eventId);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when getting event by ID that doesn't exist")
  void getEventById_WhenEventDoesNotExist_ShouldThrowEventNotFoundException() {
    // Given
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(eventId));
    verify(eventRepository).findById(eventId);
  }

  @Test
  @DisplayName("Should return events by calendar ID when events exist")
  void getEventsByCalendarId_WhenEventsExist_ShouldReturnEvents() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    List<Event> events = TestDataFactory.createEventList(3, calendar);
    when(eventRepository.findByCalendarId(calendarId)).thenReturn(events);

    // When
    List<EventDto> result = eventService.getEventsByCalendarId(calendarId);

    // Then
    assertThat(result).hasSize(3);
    verify(eventRepository).findByCalendarId(calendarId);
  }

  @Test
  @DisplayName("Should create event when creating event with valid calendar ID")
  void createEvent_WhenCalendarExists_ShouldCreateEvent() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();
    EventDto eventDto =
        TestDataFactory.createEventDto(
            "New Event", "New Description", now, now.plusHours(1), "New Location", calendarId);
    Event savedEvent =
        TestDataFactory.createEvent(
            UUID.randomUUID(),
            "New Event",
            "New Description",
            now,
            now.plusHours(1),
            "New Location",
            calendar);

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
    when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

    // When
    EventDto result = eventService.createEvent(eventDto);

    // Then
    assertThat(result.getId()).isEqualTo(savedEvent.getId());
    assertThat(result.getTitle()).isEqualTo("New Event");
    assertThat(result.getDescription()).isEqualTo("New Description");
    assertThat(result.getStartTime()).isEqualTo(now);
    assertThat(result.getEndTime()).isEqualTo(now.plusHours(1));
    assertThat(result.getLocation()).isEqualTo("New Location");
    assertThat(result.getCalendarId()).isEqualTo(calendarId);

    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(calendarRepository).findById(calendarId);
    verify(eventRepository).save(eventCaptor.capture());
    Event capturedEvent = eventCaptor.getValue();
    assertThat(capturedEvent.getTitle()).isEqualTo("New Event");
    assertThat(capturedEvent.getDescription()).isEqualTo("New Description");
    assertThat(capturedEvent.getStartTime()).isEqualTo(now);
    assertThat(capturedEvent.getEndTime()).isEqualTo(now.plusHours(1));
    assertThat(capturedEvent.getLocation()).isEqualTo("New Location");
    assertThat(capturedEvent.getCalendar()).isEqualTo(calendar);
  }

  @Test
  @DisplayName(
      "Should throw CalendarNotFoundException when creating event with invalid calendar ID")
  void createEvent_WhenCalendarDoesNotExist_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventDto =
        TestDataFactory.createEventDto(
            "New Event", "New Description", now, now.plusHours(1), "New Location", calendarId);

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(CalendarNotFoundException.class, () -> eventService.createEvent(eventDto));
    verify(calendarRepository).findById(calendarId);
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  @DisplayName("Should update event when updating event that exists")
  void updateEvent_WhenEventExists_ShouldUpdateEvent() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            calendarId);

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Original Event",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            calendar);

    Event updatedEvent =
        TestDataFactory.createEvent(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            calendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
    when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

    // When
    EventDto result = eventService.updateEvent(eventId, eventDto);

    // Then
    assertThat(result.getId()).isEqualTo(eventId);
    assertThat(result.getTitle()).isEqualTo("Updated Event");
    assertThat(result.getDescription()).isEqualTo("Updated Description");
    assertThat(result.getStartTime()).isEqualTo(now.plusHours(2));
    assertThat(result.getEndTime()).isEqualTo(now.plusHours(3));
    assertThat(result.getLocation()).isEqualTo("Updated Location");
    assertThat(result.getCalendarId()).isEqualTo(calendarId);

    verify(eventRepository).findById(eventId);
    verify(eventRepository).save(existingEvent);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when updating event that doesn't exist")
  void updateEvent_WhenEventDoesNotExist_ShouldThrowEventNotFoundException() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            calendarId);

    when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(eventId, eventDto));
    verify(eventRepository).findById(eventId);
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  @DisplayName("Should throw ConcurrentModificationException when version mismatch")
  void updateEvent_WhenVersionMismatch_ShouldThrowConcurrentModificationException() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            calendarId,
            2L);

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Original Event",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            calendar,
            1L);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

    // When/Then
    assertThrows(
        ConcurrentModificationException.class, () -> eventService.updateEvent(eventId, eventDto));
    verify(eventRepository).findById(eventId);
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  @DisplayName("Should update calendar when updating event with different calendar ID")
  void updateEvent_WhenCalendarChanges_ShouldUpdateCalendar() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID oldCalendarId = UUID.randomUUID();
    UUID newCalendarId = UUID.randomUUID();

    Calendar oldCalendar =
        TestDataFactory.createCalendar(oldCalendarId, "Old Calendar", "Old Description", null);
    Calendar newCalendar =
        TestDataFactory.createCalendar(newCalendarId, "New Calendar", "New Description", null);

    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            newCalendarId);

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Original Event",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            oldCalendar);

    Event updatedEvent =
        TestDataFactory.createEvent(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            newCalendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
    when(calendarRepository.findById(newCalendarId)).thenReturn(Optional.of(newCalendar));
    when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

    // When
    EventDto result = eventService.updateEvent(eventId, eventDto);

    // Then
    assertThat(result.getId()).isEqualTo(eventId);
    assertThat(result.getCalendarId()).isEqualTo(newCalendarId);

    verify(eventRepository).findById(eventId);
    verify(calendarRepository).findById(newCalendarId);
    verify(eventRepository).save(existingEvent);
  }

  @Test
  @DisplayName(
      "Should throw CalendarNotFoundException when updating event with invalid calendar ID")
  void updateEvent_WhenNewCalendarDoesNotExist_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID oldCalendarId = UUID.randomUUID();
    UUID newCalendarId = UUID.randomUUID();

    Calendar oldCalendar =
        TestDataFactory.createCalendar(oldCalendarId, "Old Calendar", "Old Description", null);

    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            newCalendarId);

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Original Event",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            oldCalendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
    when(calendarRepository.findById(newCalendarId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        CalendarNotFoundException.class, () -> eventService.updateEvent(eventId, eventDto));
    verify(eventRepository).findById(eventId);
    verify(calendarRepository).findById(newCalendarId);
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  @DisplayName("Should delete event when deleting event that exists")
  void deleteEvent_WhenEventExists_ShouldDeleteEvent() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Event To Delete",
            "Delete Description",
            now,
            now.plusHours(1),
            "Delete Location",
            calendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

    // When
    eventService.deleteEvent(eventId);

    // Then
    verify(eventRepository).findById(eventId);
    verify(eventRepository).delete(existingEvent);
  }

  @Test
  @DisplayName("Should throw EventNotFoundException when deleting event that doesn't exist")
  void deleteEvent_WhenEventDoesNotExist_ShouldThrowEventNotFoundException() {
    // Given
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(EventNotFoundException.class, () -> eventService.deleteEvent(eventId));
    verify(eventRepository).findById(eventId);
    verify(eventRepository, never()).delete(any(Event.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when creating event")
  void createEvent_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();
    EventDto eventDto =
        TestDataFactory.createEventDto(
            "New Event", "New Description", now, now.plusHours(1), "New Location", calendarId);

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
    when(eventRepository.save(any(Event.class))).thenThrow(OptimisticLockingFailureException.class);

    // When/Then
    assertThrows(ConcurrentModificationException.class, () -> eventService.createEvent(eventDto));
    verify(calendarRepository).findById(calendarId);
    verify(eventRepository).save(any(Event.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when updating event")
  void updateEvent_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();

    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Updated Event",
            "Updated Description",
            now.plusHours(2),
            now.plusHours(3),
            "Updated Location",
            calendarId);

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Original Event",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            calendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
    when(eventRepository.save(any(Event.class))).thenThrow(OptimisticLockingFailureException.class);

    // When/Then
    assertThrows(
        ConcurrentModificationException.class, () -> eventService.updateEvent(eventId, eventDto));
    verify(eventRepository).findById(eventId);
    verify(eventRepository).save(any(Event.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when deleting event")
  void deleteEvent_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(calendarId, "Test Calendar", "Test Description", null);
    LocalDateTime now = LocalDateTime.now();

    Event existingEvent =
        TestDataFactory.createEvent(
            eventId,
            "Event To Delete",
            "Delete Description",
            now,
            now.plusHours(1),
            "Delete Location",
            calendar);

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
    doThrow(OptimisticLockingFailureException.class).when(eventRepository).delete(any(Event.class));

    // When/Then
    assertThrows(ConcurrentModificationException.class, () -> eventService.deleteEvent(eventId));
    verify(eventRepository).findById(eventId);
    verify(eventRepository).delete(any(Event.class));
  }

  @Test
  @DisplayName("Should detect validation error when creating event with end time before start time")
  void createEvent_WhenEndTimeBeforeStartTime_ShouldDetectValidationError() {
    // Given
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    // Create an event with end time before start time
    EventDto eventDto =
        TestDataFactory.createEventDto(
            "Invalid Event",
            "Invalid Time Range",
            now,
            now.minusHours(1), // End time is before start time
            "Test Location",
            calendarId);

    // When
    Set<ConstraintViolation<EventDto>> violations = validator.validate(eventDto);

    // Then
    assertThat(violations).isNotEmpty();
    assertThat(violations.size()).isEqualTo(1);
    ConstraintViolation<EventDto> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("End time must be after start time");
  }

  @Test
  @DisplayName("Should detect validation error when updating event with end time before start time")
  void updateEvent_WhenEndTimeBeforeStartTime_ShouldDetectValidationError() {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    // Create an event with end time before start time
    EventDto eventDto =
        TestDataFactory.createEventDto(
            eventId,
            "Invalid Event",
            "Invalid Time Range",
            now,
            now.minusHours(1), // End time is before start time
            "Test Location",
            calendarId);

    // When
    Set<ConstraintViolation<EventDto>> violations = validator.validate(eventDto);

    // Then
    assertThat(violations).isNotEmpty();
    assertThat(violations.size()).isEqualTo(1);
    ConstraintViolation<EventDto> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("End time must be after start time");
  }
}
