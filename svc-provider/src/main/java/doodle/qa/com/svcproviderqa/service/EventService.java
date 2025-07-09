package doodle.qa.com.svcproviderqa.service;

import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.entity.Event;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.exception.ConcurrentModificationException;
import doodle.qa.com.svcproviderqa.exception.EventNotFoundException;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.repository.EventRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
   * Retrieves events by calendar ID.
   *
   * @param calendarId The calendar ID
   * @return List of EventDto objects for the specified calendar ID
   */
  public List<EventDto> getEventsByCalendarId(@NotNull UUID calendarId) {
    log.debug("Retrieving events for calendar id: {}", calendarId);
    return eventRepository.findByCalendarId(calendarId).stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }


  /**
   * Retrieves events for a calendar within a time range.
   *
   * @param calendarId The calendar ID
   * @param start Start time
   * @param end End time
   * @return List of EventDto objects for the specified calendar within the time range
   */
  public List<EventDto> getEventsByCalendarIdAndTimeRange(
      @NotNull UUID calendarId, @NotNull LocalDateTime start, @NotNull LocalDateTime end) {
    log.debug("Retrieving events for calendar {} between {} and {}", calendarId, start, end);
    return eventRepository.findByCalendarIdAndStartTimeBetween(calendarId, start, end).stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  /**
   * Creates a new event.
   *
   * @param eventDto The event data
   * @return EventDto for the created event
   * @throws CalendarNotFoundException if the calendar is not found
   * @throws ConcurrentModificationException if there's a conflict during creation
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public EventDto createEvent(@NotNull @Valid EventDto eventDto) {
    log.debug("Creating event with title: {}", eventDto.getTitle());

    try {
      // Find the calendar
      Calendar calendar =
          calendarRepository
              .findById(eventDto.getCalendarId())
              .orElseThrow(
                  () -> {
                    log.warn("Calendar not found with id: {}", eventDto.getCalendarId());
                    return new CalendarNotFoundException(eventDto.getCalendarId());
                  });

      Event event =
          Event.builder()
              .title(eventDto.getTitle())
              .description(eventDto.getDescription())
              .startTime(eventDto.getStartTime())
              .endTime(eventDto.getEndTime())
              .location(eventDto.getLocation())
              .calendar(calendar)
              .build();

      // Add the event to the calendar's events list
      calendar.addEvent(event);

      Event savedEvent = eventRepository.save(event);

      log.info("Event created: {}", savedEvent.getId());
      return mapToDto(savedEvent);
    } catch (OptimisticLockingFailureException e) {
      log.warn(
          "Concurrent modification detected while creating event with title: {}",
          eventDto.getTitle(),
          e);
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
   * @throws CalendarNotFoundException if the calendar is not found
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

      // If calendar ID has changed, find the new calendar
      if (!event.getCalendar().getId().equals(eventDto.getCalendarId())) {
        Calendar calendar =
            calendarRepository
                .findById(eventDto.getCalendarId())
                .orElseThrow(
                    () -> {
                      log.warn("Calendar not found with id: {}", eventDto.getCalendarId());
                      return new CalendarNotFoundException(eventDto.getCalendarId());
                    });
        event.setCalendar(calendar);
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
        .version(event.getVersion())
        .calendarId(event.getCalendar() != null ? event.getCalendar().getId() : null)
        .build();
  }
}
