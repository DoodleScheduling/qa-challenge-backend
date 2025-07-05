package doodle.qa.com.svccalendarqa.service;

import doodle.qa.com.svccalendarqa.dto.EventDto;
import doodle.qa.com.svccalendarqa.entity.Calendar;
import doodle.qa.com.svccalendarqa.entity.Event;
import doodle.qa.com.svccalendarqa.exception.CalendarNotFoundException;
import doodle.qa.com.svccalendarqa.exception.ConcurrentModificationException;
import doodle.qa.com.svccalendarqa.exception.EventNotFoundException;
import doodle.qa.com.svccalendarqa.repository.CalendarRepository;
import doodle.qa.com.svccalendarqa.repository.EventRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional(readOnly = true)
public class EventService {

  private final EventRepository eventRepository;
  private final CalendarRepository calendarRepository;

  /**
   * Retrieves all events for a specific calendar.
   *
   * @param calendarId The calendar ID
   * @return List of EventDto objects in the calendar
   */
  public List<EventDto> getEventsByCalendarId(@NotNull UUID calendarId) {
    log.debug("Retrieving events for calendar: {}", calendarId);

    // Verify calendar exists
    if (!calendarRepository.existsById(calendarId)) {
      throw new CalendarNotFoundException(calendarId);
    }

    List<Event> events = eventRepository.findByCalendarIdOrderByStartTime(calendarId);
    return events.stream().map(this::mapToDto).collect(Collectors.toList());
  }

  /**
   * Retrieves events for a calendar within a time range.
   *
   * @param calendarId The calendar ID
   * @param startTime The start time of the range
   * @param endTime The end time of the range
   * @return List of EventDto objects within the time range
   */
  public List<EventDto> getEventsByCalendarIdAndTimeRange(
      @NotNull UUID calendarId, @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {
    log.debug("Retrieving events for calendar: {} from {} to {}", calendarId, startTime, endTime);

    // Verify calendar exists
    if (!calendarRepository.existsById(calendarId)) {
      throw new CalendarNotFoundException(calendarId);
    }

    List<Event> events =
        eventRepository.findByCalendarIdAndTimeRange(calendarId, startTime, endTime);
    return events.stream().map(this::mapToDto).collect(Collectors.toList());
  }

  /**
   * Retrieves an event by ID.
   *
   * @param id The event ID
   * @return EventDto for the specified ID
   * @throws EventNotFoundException if event not found
   */
  public EventDto getEventById(@NotNull UUID id) {
    log.debug("Retrieving event with id: {}", id);
    return eventRepository
        .findById(id)
        .map(this::mapToDto)
        .orElseThrow(
            () -> {
              log.warn("Event not found with id: {}", id);
              return new EventNotFoundException(id);
            });
  }

  /**
   * Creates a new event.
   *
   * @param eventDto The event data
   * @return EventDto for the created event
   * @throws ConcurrentModificationException if there's a conflict during creation
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public EventDto createEvent(@NotNull @Valid EventDto eventDto) {
    log.debug("Creating event: {}", eventDto.getTitle());

    // Verify calendar exists
    Calendar calendar =
        calendarRepository
            .findById(eventDto.getCalendarId())
            .orElseThrow(() -> new CalendarNotFoundException(eventDto.getCalendarId()));

    try {
      Event event =
          Event.builder()
              .title(eventDto.getTitle())
              .description(eventDto.getDescription())
              .startTime(eventDto.getStartTime())
              .endTime(eventDto.getEndTime())
              .location(eventDto.getLocation())
              .calendar(calendar)
              .build();

      Event savedEvent = eventRepository.save(event);
      log.info("Event created: {}", savedEvent.getId());
      return mapToDto(savedEvent);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while creating event: {}", eventDto.getTitle(), e);
      throw new ConcurrentModificationException(
          "A conflict occurred while creating the event. Please try again.", e);
    }
  }

  /**
   * Updates an existing event.
   *
   * @param id The event ID
   * @param eventDto The updated event data
   * @return EventDto for the updated event
   * @throws EventNotFoundException if event not found
   * @throws ConcurrentModificationException if the event was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public EventDto updateEvent(@NotNull UUID id, @NotNull @Valid EventDto eventDto) {
    log.debug("Updating event with id: {}", id);

    try {
      Event event =
          eventRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("Event not found with id: {}", id);
                    return new EventNotFoundException(id);
                  });

      // Check if version matches to ensure optimistic locking
      if (eventDto.getVersion() != null
          && !Objects.equals(event.getVersion(), eventDto.getVersion())) {
        log.warn(
            "Version mismatch detected while updating event with id: {}. Expected: {}, Actual: {}",
            id,
            eventDto.getVersion(),
            event.getVersion());
        throw new ConcurrentModificationException(
            "The event was modified by another operation. Please refresh and try again.");
      }

      event.setTitle(eventDto.getTitle());
      event.setDescription(eventDto.getDescription());
      event.setStartTime(eventDto.getStartTime());
      event.setEndTime(eventDto.getEndTime());
      event.setLocation(eventDto.getLocation());

      Event updatedEvent = eventRepository.save(event);
      log.info("Event updated: {}", updatedEvent.getId());
      return mapToDto(updatedEvent);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while updating event with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The event was modified by another operation. Please refresh and try again.", e);
    }
  }

  /**
   * Deletes an event.
   *
   * @param id The event ID
   * @throws EventNotFoundException if event not found
   * @throws ConcurrentModificationException if the event was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public void deleteEvent(@NotNull UUID id) {
    log.debug("Deleting event with id: {}", id);

    try {
      Event event =
          eventRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("Event not found with id: {}", id);
                    return new EventNotFoundException(id);
                  });

      eventRepository.delete(event);
      log.info("Event deleted: {}", id);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while deleting event with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The event was modified by another operation. Please refresh and try again.", e);
    }
  }

  /**
   * Maps an Event entity to an EventDto.
   *
   * @param event The Event entity
   * @return EventDto with copied data
   */
  private EventDto mapToDto(Event event) {
    if (event == null) {
      return null;
    }

    return EventDto.builder()
        .id(event.getId())
        .title(event.getTitle())
        .description(event.getDescription())
        .startTime(event.getStartTime())
        .endTime(event.getEndTime())
        .location(event.getLocation())
        .calendarId(event.getCalendar().getId())
        .version(event.getVersion())
        .createdAt(event.getCreatedAt())
        .updatedAt(event.getUpdatedAt())
        .build();
  }
}
