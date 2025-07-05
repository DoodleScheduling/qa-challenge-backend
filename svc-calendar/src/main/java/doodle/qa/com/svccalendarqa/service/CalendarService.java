package doodle.qa.com.svccalendarqa.service;

import doodle.qa.com.svccalendarqa.dto.CalendarDto;
import doodle.qa.com.svccalendarqa.dto.EventDto;
import doodle.qa.com.svccalendarqa.entity.Calendar;
import doodle.qa.com.svccalendarqa.entity.Event;
import doodle.qa.com.svccalendarqa.exception.CalendarNotFoundException;
import doodle.qa.com.svccalendarqa.exception.ConcurrentModificationException;
import doodle.qa.com.svccalendarqa.exception.UserNotFoundException;
import doodle.qa.com.svccalendarqa.repository.CalendarRepository;
import doodle.qa.com.svccalendarqa.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class CalendarService {

  private final CalendarRepository calendarRepository;
  private final UserRepository userRepository;

  /**
   * Retrieves all calendars for a specific user.
   *
   * @param userId The user ID
   * @return List of CalendarDto objects owned by the user
   */
  public List<CalendarDto> getCalendarsByUserId(@NotNull UUID userId) {
    log.debug("Retrieving calendars for user: {}", userId);

    // Verify user exists
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }

    List<Calendar> calendars = calendarRepository.findByOwnerIdOrderByName(userId);
    return calendars.stream().map(this::mapToDto).collect(Collectors.toList());
  }

  /**
   * Retrieves a calendar by ID.
   *
   * @param id The calendar ID
   * @return CalendarDto for the specified ID
   * @throws CalendarNotFoundException if calendar not found
   */
  public CalendarDto getCalendarById(@NotNull UUID id) {
    log.debug("Retrieving calendar with id: {}", id);
    return calendarRepository
        .findById(id)
        .map(this::mapToDto)
        .orElseThrow(
            () -> {
              log.warn("Calendar not found with id: {}", id);
              return new CalendarNotFoundException(id);
            });
  }

  /**
   * Creates a new calendar.
   *
   * @param calendarDto The calendar data
   * @return CalendarDto for the created calendar
   * @throws ConcurrentModificationException if there's a conflict during creation
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public CalendarDto createCalendar(@NotNull @Valid CalendarDto calendarDto) {
    log.debug("Creating calendar: {}", calendarDto.getName());

    // Verify owner exists
    if (!userRepository.existsById(calendarDto.getOwnerId())) {
      throw new UserNotFoundException(calendarDto.getOwnerId());
    }

    try {
      Calendar calendar =
          Calendar.builder()
              .name(calendarDto.getName())
              .description(calendarDto.getDescription())
              .ownerId(calendarDto.getOwnerId())
              .build();

      Calendar savedCalendar = calendarRepository.save(calendar);
      log.info("Calendar created: {}", savedCalendar.getId());
      return mapToDto(savedCalendar);
    } catch (OptimisticLockingFailureException e) {
      log.warn(
          "Concurrent modification detected while creating calendar: {}", calendarDto.getName(), e);
      throw new ConcurrentModificationException(
          "A conflict occurred while creating the calendar. Please try again.", e);
    }
  }

  /**
   * Updates an existing calendar.
   *
   * @param id The calendar ID
   * @param calendarDto The updated calendar data
   * @return CalendarDto for the updated calendar
   * @throws CalendarNotFoundException if calendar not found
   * @throws ConcurrentModificationException if the calendar was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public CalendarDto updateCalendar(@NotNull UUID id, @NotNull @Valid CalendarDto calendarDto) {
    log.debug("Updating calendar with id: {}", id);

    try {
      Calendar calendar =
          calendarRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("Calendar not found with id: {}", id);
                    return new CalendarNotFoundException(id);
                  });

      // Check if version matches to ensure optimistic locking
      if (calendarDto.getVersion() != null
          && !Objects.equals(calendar.getVersion(), calendarDto.getVersion())) {
        log.warn(
            "Version mismatch detected while updating calendar with id: {}. Expected: {}, Actual: {}",
            id,
            calendarDto.getVersion(),
            calendar.getVersion());
        throw new ConcurrentModificationException(
            "The calendar was modified by another operation. Please refresh and try again.");
      }

      calendar.setName(calendarDto.getName());
      calendar.setDescription(calendarDto.getDescription());

      Calendar updatedCalendar = calendarRepository.save(calendar);
      log.info("Calendar updated: {}", updatedCalendar.getId());
      return mapToDto(updatedCalendar);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while updating calendar with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The calendar was modified by another operation. Please refresh and try again.", e);
    }
  }

  /**
   * Deletes a calendar.
   *
   * @param id The calendar ID
   * @throws CalendarNotFoundException if calendar not found
   * @throws ConcurrentModificationException if the calendar was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public void deleteCalendar(@NotNull UUID id) {
    log.debug("Deleting calendar with id: {}", id);

    try {
      Calendar calendar =
          calendarRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("Calendar not found with id: {}", id);
                    return new CalendarNotFoundException(id);
                  });

      calendarRepository.delete(calendar);
      log.info("Calendar deleted: {}", id);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while deleting calendar with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The calendar was modified by another operation. Please refresh and try again.", e);
    }
  }

  /**
   * Maps a Calendar entity to a CalendarDto.
   *
   * @param calendar The Calendar entity
   * @return CalendarDto with copied data
   */
  private CalendarDto mapToDto(Calendar calendar) {
    if (calendar == null) {
      return null;
    }

    List<EventDto> eventDtos =
        calendar.getEvents().stream().map(this::mapEventToDto).collect(Collectors.toList());

    return CalendarDto.builder()
        .id(calendar.getId())
        .name(calendar.getName())
        .description(calendar.getDescription())
        .ownerId(calendar.getOwnerId())
        .version(calendar.getVersion())
        .createdAt(calendar.getCreatedAt())
        .updatedAt(calendar.getUpdatedAt())
        .events(eventDtos)
        .build();
  }

  /**
   * Maps an Event entity to an EventDto.
   *
   * @param event The Event entity
   * @return EventDto with copied data
   */
  private EventDto mapEventToDto(Event event) {
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
