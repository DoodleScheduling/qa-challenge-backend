package doodle.qa.com.svccalendarqa.service;

import doodle.qa.com.svccalendarqa.dto.MeetingDto;
import doodle.qa.com.svccalendarqa.dto.TimeSlotDto;
import doodle.qa.com.svccalendarqa.entity.Meeting;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import doodle.qa.com.svccalendarqa.exception.CalendarNotFoundException;
import doodle.qa.com.svccalendarqa.exception.IllegalArgumentException;
import doodle.qa.com.svccalendarqa.exception.MeetingNotFoundException;
import doodle.qa.com.svccalendarqa.repository.MeetingRepository;
import doodle.qa.com.svccalendarqa.repository.UserCalendarRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

/** Service for managing meetings. */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

  private final MeetingRepository meetingRepository;
  private final UserCalendarRepository userCalendarRepository;
  private final RestTemplate restTemplate;

  @Value("${provider.service.url:http://localhost:8083}")
  private String providerServiceUrl;

  /** Maximum allowed time range in days. */
  private static final int MAX_TIME_RANGE_DAYS = 7;

  /** Maximum allowed slot duration in hours. */
  private static final int MAX_SLOT_DURATION_HOURS = 8;

  /**
   * Find meetings by user ID, calendar ID, and time range.
   *
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @param from the start time
   * @param to the end time
   * @param pageable the pagination information
   * @return a page of meetings
   */
  @Transactional(readOnly = true)
  public Page<MeetingDto> findMeetings(
      @NotNull UUID userId,
      @NotNull UUID calendarId,
      @NotNull LocalDateTime from,
      @NotNull LocalDateTime to,
      Pageable pageable) {

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Validate time range
    validateTimeRange(from, to);

    // Find meetings
    Page<Meeting> meetings =
        meetingRepository
            .findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
                userCalendar, from, to, pageable);
    return meetings.map(this::mapToDto);
  }

  /**
   * Find available time slots by user ID, calendar ID, time range, and slot duration.
   *
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @param from the start time
   * @param to the end time
   * @param slotDurationMinutes the slot duration in minutes
   * @param pageable the pagination information
   * @return a list of available time slots
   */
  @Transactional(readOnly = true)
  public Page<TimeSlotDto> findAvailableTimeSlots(
      @NotNull UUID userId,
      @NotNull UUID calendarId,
      @NotNull LocalDateTime from,
      @NotNull LocalDateTime to,
      @Min(15) @Max(MAX_SLOT_DURATION_HOURS * 60) int slotDurationMinutes,
      Pageable pageable) {

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Validate time range
    validateTimeRange(from, to);

    // Get busy slots from meetings
    List<Meeting> busyMeetings =
        meetingRepository.findOverlappingMeetingsByUserCalendar(userCalendar, from, to);

    // Get busy slots from provider service
    List<Map<String, Object>> externalEvents = getExternalEvents(calendarId, from, to);

    // Combine busy slots
    List<TimeSlotDto> busySlots = new ArrayList<>();

    // Add busy slots from meetings
    for (Meeting meeting : busyMeetings) {
      busySlots.add(
          TimeSlotDto.builder()
              .startTime(meeting.getStartTime())
              .endTime(meeting.getEndTime())
              .durationMinutes(
                  (int) ChronoUnit.MINUTES.between(meeting.getStartTime(), meeting.getEndTime()))
              .build());
    }

    // Add busy slots from external events
    for (Map<String, Object> event : externalEvents) {
      LocalDateTime eventStart = LocalDateTime.parse((String) event.get("startTime"));
      LocalDateTime eventEnd = LocalDateTime.parse((String) event.get("endTime"));

      busySlots.add(
          TimeSlotDto.builder()
              .startTime(eventStart)
              .endTime(eventEnd)
              .durationMinutes((int) ChronoUnit.MINUTES.between(eventStart, eventEnd))
              .build());
    }

    // Sort busy slots by start time
    busySlots.sort(Comparator.comparing(TimeSlotDto::getStartTime));

    // Find available slots
    List<TimeSlotDto> availableSlots = findAvailableSlots(from, to, slotDurationMinutes, busySlots);

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), availableSlots.size());
    List<TimeSlotDto> paginatedSlots = availableSlots.subList(start, end);

    // Create a Page object
    return new org.springframework.data.domain.PageImpl<>(
        paginatedSlots, pageable, availableSlots.size());
  }

  /**
   * Find a meeting by ID, user ID, and calendar ID.
   *
   * @param meetingId the meeting ID
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @return the meeting
   */
  @Transactional(readOnly = true)
  public MeetingDto findMeeting(
      @NotNull UUID meetingId, @NotNull UUID userId, @NotNull UUID calendarId) {

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Find meeting
    Meeting meeting =
        meetingRepository
            .findByUserCalendarAndId(userCalendar, meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId, userId, calendarId));

    return mapToDto(meeting);
  }

  /**
   * Create a meeting.
   *
   * @param meetingDto the meeting DTO
   * @param userId the user ID
   * @return the created meeting
   */
  @Transactional
  @Retryable(
      value = OptimisticLockException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public MeetingDto createMeeting(@Valid @NotNull MeetingDto meetingDto, @NotNull UUID userId) {

    UUID calendarId = meetingDto.getCalendarId();

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Validate meeting time
    validateMeetingTime(meetingDto.getStartTime(), meetingDto.getEndTime());

    // Check for conflicts
    checkForConflicts(calendarId, meetingDto.getStartTime(), meetingDto.getEndTime(), null);

    // Create meeting
    Meeting meeting =
        Meeting.builder()
            .title(meetingDto.getTitle())
            .description(meetingDto.getDescription())
            .startTime(meetingDto.getStartTime())
            .endTime(meetingDto.getEndTime())
            .location(meetingDto.getLocation())
            .userCalendar(userCalendar)
            .calendarId(calendarId)
            .build();

    Meeting savedMeeting = meetingRepository.save(meeting);

    return mapToDto(savedMeeting);
  }

  /**
   * Update a meeting.
   *
   * @param meetingId the meeting ID
   * @param meetingDto the meeting DTO
   * @param userId the user ID
   * @return the updated meeting
   */
  @Transactional
  @Retryable(
      value = OptimisticLockException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public MeetingDto updateMeeting(
      @NotNull UUID meetingId, @Valid @NotNull MeetingDto meetingDto, @NotNull UUID userId) {

    UUID calendarId = meetingDto.getCalendarId();

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Find meeting
    Meeting meeting =
        meetingRepository
            .findByUserCalendarAndId(userCalendar, meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId, userId, calendarId));

    // Validate meeting time
    validateMeetingTime(meetingDto.getStartTime(), meetingDto.getEndTime());

    // Check for conflicts (excluding this meeting)
    checkForConflicts(calendarId, meetingDto.getStartTime(), meetingDto.getEndTime(), meetingId);

    // Update meeting
    meeting.setTitle(meetingDto.getTitle());
    meeting.setDescription(meetingDto.getDescription());
    meeting.setStartTime(meetingDto.getStartTime());
    meeting.setEndTime(meetingDto.getEndTime());
    meeting.setLocation(meetingDto.getLocation());

    Meeting savedMeeting = meetingRepository.save(meeting);
    return mapToDto(savedMeeting);
  }

  /**
   * Delete a meeting.
   *
   * @param meetingId the meeting ID
   * @param userId the user ID
   * @param calendarId the calendar ID
   */
  @Transactional
  public void deleteMeeting(
      @NotNull UUID meetingId, @NotNull UUID userId, @NotNull UUID calendarId) {

    // Validate user and calendar
    UserCalendar userCalendar = validateUserAndCalendar(userId, calendarId);

    // Find meeting
    Meeting meeting =
        meetingRepository
            .findByUserCalendarAndId(userCalendar, meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId, userId, calendarId));

    // Delete meeting
    meetingRepository.delete(meeting);
  }

  /**
   * Validate that the user calendar exists for the specified user.
   *
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @throws CalendarNotFoundException if the calendar is not found for the user
   */
  private UserCalendar validateUserAndCalendar(UUID userId, UUID calendarId) {
    return userCalendarRepository
        .findByCalendarIdAndUserId(calendarId, userId)
        .orElseThrow(() -> new CalendarNotFoundException(calendarId, userId));
  }

  /**
   * Validate that the time range is valid and does not exceed the maximum allowed range.
   *
   * @param from the start time
   * @param to the end time
   * @throws IllegalArgumentException if the time range is invalid
   */
  private void validateTimeRange(LocalDateTime from, LocalDateTime to) {
    if (from.isAfter(to)) {
      throw new IllegalArgumentException(
          "Start time {" + from + "} must be before end time {" + to + "}");
    }

    Duration duration = Duration.between(from, to).abs();
    if (duration.compareTo(Duration.ofDays(MAX_TIME_RANGE_DAYS)) > 0) {
      throw new IllegalArgumentException(
          "Time range cannot exceed " + MAX_TIME_RANGE_DAYS + " days");
    }
  }

  /**
   * Validate that the meeting time is valid and the duration does not exceed the maximum allowed
   * duration.
   *
   * @param startTime the start time
   * @param endTime the end time
   * @throws IllegalArgumentException if the meeting time is invalid
   */
  private void validateMeetingTime(LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime.isAfter(endTime)) {
      throw new IllegalArgumentException(
          "Start time {" + startTime + "} must be before end time {" + endTime + "}");
    }

    Duration duration = Duration.between(startTime, endTime).abs();
    if (duration.compareTo(Duration.ofHours(MAX_SLOT_DURATION_HOURS)) > 0) {
      throw new IllegalArgumentException(
          "Meeting duration cannot exceed " + MAX_SLOT_DURATION_HOURS + " hours");
    }
  }

  /**
   * Check for conflicts with existing meetings.
   *
   * @param calendarId the calendar ID
   * @param startTime the start time
   * @param endTime the end time
   * @param excludeMeetingId the meeting ID to exclude from the check (for updates)
   * @throws IllegalArgumentException if there are conflicts
   */
  private void checkForConflicts(
      UUID calendarId, LocalDateTime startTime, LocalDateTime endTime, UUID excludeMeetingId) {
    // Get all UserCalendars for this calendar ID
    List<UserCalendar> userCalendars = userCalendarRepository.findAllByCalendarId(calendarId);

    if (userCalendars.isEmpty()) {
      throw new CalendarNotFoundException(calendarId);
    }

    List<Meeting> allOverlappingMeetings = new ArrayList<>();

    // Check for conflicts in all user calendars with this calendar ID
    for (UserCalendar userCalendar : userCalendars) {
      List<Meeting> overlappingMeetings =
          meetingRepository.findOverlappingMeetingsByUserCalendar(userCalendar, startTime, endTime);

      // Filter out the meeting being updated
      if (excludeMeetingId != null) {
        overlappingMeetings =
            overlappingMeetings.stream()
                .filter(meeting -> !meeting.getId().equals(excludeMeetingId))
                .toList();
      }

      allOverlappingMeetings.addAll(overlappingMeetings);
    }

    if (!allOverlappingMeetings.isEmpty()) {
      throw new IllegalArgumentException("The meeting conflicts with existing meetings");
    }

    // Check for conflicts with external events
    List<Map<String, Object>> externalEvents = getExternalEvents(calendarId, startTime, endTime);

    for (Map<String, Object> event : externalEvents) {
      LocalDateTime eventStart = LocalDateTime.parse((String) event.get("startTime"));
      LocalDateTime eventEnd = LocalDateTime.parse((String) event.get("endTime"));

      if (!(endTime.isBefore(eventStart) || startTime.isAfter(eventEnd))) {
        throw new IllegalArgumentException("The meeting conflicts with external events");
      }
    }
  }

  /**
   * Get external events from the provider service.
   *
   * @param calendarId the calendar ID
   * @param from the start time
   * @param to the end time
   * @return a list of external events
   */
  @Retryable(
      value = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  private List<Map<String, Object>> getExternalEvents(
      UUID calendarId, LocalDateTime from, LocalDateTime to) {
    try {
      String url =
          String.format(
              "%s/api/events/calendar/%s/timerange?start=%s&end=%s",
              providerServiceUrl, calendarId, from, to);

      ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

      if (response.getBody() != null) {
        return response.getBody();
      }
    } catch (Exception e) {
      log.error("Error getting external events", e);
    }

    return Collections.emptyList();
  }

  /**
   * Find available time slots given busy slots.
   *
   * @param from the start time
   * @param to the end time
   * @param slotDurationMinutes the slot duration in minutes
   * @param busySlots the busy slots
   * @return a list of available time slots
   */
  private List<TimeSlotDto> findAvailableSlots(
      LocalDateTime from, LocalDateTime to, int slotDurationMinutes, List<TimeSlotDto> busySlots) {

    List<TimeSlotDto> availableSlots = new ArrayList<>();
    LocalDateTime current = from;
    Duration slotDuration = Duration.ofMinutes(slotDurationMinutes);

    // If there are no busy slots, return the entire range as available
    if (busySlots.isEmpty()) {
      while (current.plus(slotDuration).isBefore(to) || current.plus(slotDuration).isEqual(to)) {
        availableSlots.add(
            TimeSlotDto.builder()
                .startTime(current)
                .endTime(current.plus(slotDuration))
                .durationMinutes(slotDurationMinutes)
                .build());

        current = current.plus(slotDuration);
      }

      return availableSlots;
    }

    // Find available slots between busy slots
    for (TimeSlotDto busySlot : busySlots) {
      // Add available slots before the busy slot
      while (current.plus(slotDuration).isBefore(busySlot.getStartTime())
          || current.plus(slotDuration).isEqual(busySlot.getStartTime())) {
        availableSlots.add(
            TimeSlotDto.builder()
                .startTime(current)
                .endTime(current.plus(slotDuration))
                .durationMinutes(slotDurationMinutes)
                .build());

        current = current.plus(slotDuration);
      }

      // Move current to the end of the busy slot
      current = busySlot.getEndTime();
    }

    // Add available slots after the last busy slot
    while (current.plus(slotDuration).isBefore(to) || current.plus(slotDuration).isEqual(to)) {
      availableSlots.add(
          TimeSlotDto.builder()
              .startTime(current)
              .endTime(current.plus(slotDuration))
              .durationMinutes(slotDurationMinutes)
              .build());

      current = current.plus(slotDuration);
    }

    return availableSlots;
  }

  private MeetingDto mapToDto(Meeting meeting) {
    if (meeting == null) {
      return null;
    }

    return MeetingDto.builder()
        .title(meeting.getTitle())
        .description(meeting.getDescription())
        .startTime(meeting.getStartTime())
        .endTime(meeting.getEndTime())
        .location(meeting.getLocation())
        .calendarId(meeting.getCalendarId())
        .id(meeting.getId())
        .build();
  }
}
