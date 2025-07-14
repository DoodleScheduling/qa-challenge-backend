package doodle.qa.com.svccalendarqa;

import doodle.qa.com.svccalendarqa.dto.MeetingDto;
import doodle.qa.com.svccalendarqa.dto.TimeSlotDto;
import doodle.qa.com.svccalendarqa.entity.Meeting;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects. This class provides methods to create test
 * meetings, user calendars, and DTOs with predefined or random data.
 */
public class TestDataFactory {

  /**
   * Creates a Meeting entity with the given parameters.
   *
   * @param id The meeting ID
   * @param title The meeting title
   * @param description The meeting description
   * @param startTime The meeting start time
   * @param endTime The meeting end time
   * @param location The meeting location
   * @param userCalendar The user calendar
   * @param calendarId The calendar ID
   * @param version The version for optimistic locking
   * @return A Meeting entity
   */
  public static Meeting createMeeting(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UserCalendar userCalendar,
      UUID calendarId,
      Long version) {
    return Meeting.builder()
        .id(id)
        .title(title)
        .description(description)
        .startTime(startTime)
        .endTime(endTime)
        .location(location)
        .userCalendar(userCalendar)
        .calendarId(calendarId)
        .version(version)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * Creates a Meeting entity with the given parameters. The version will be null, which is
   * appropriate for new meetings that haven't been persisted yet.
   *
   * @param id The meeting ID
   * @param title The meeting title
   * @param description The meeting description
   * @param startTime The meeting start time
   * @param endTime The meeting end time
   * @param location The meeting location
   * @param userCalendar The user calendar
   * @param calendarId The calendar ID
   * @return A Meeting entity
   */
  public static Meeting createMeeting(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UserCalendar userCalendar,
      UUID calendarId) {
    return createMeeting(
        id, title, description, startTime, endTime, location, userCalendar, calendarId, null);
  }

  /**
   * Creates a Meeting entity with default values for testing.
   *
   * @param userCalendar The user calendar
   * @param calendarId The calendar ID
   * @return A Meeting entity
   */
  public static Meeting createDefaultMeeting(UserCalendar userCalendar, UUID calendarId) {
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    return createMeeting(
        null,
        "Test Meeting",
        "Test Description",
        now,
        now.plusHours(1),
        "Test Location",
        userCalendar,
        calendarId);
  }

  /**
   * Creates a MeetingDto with the given parameters.
   *
   * @param id The meeting ID
   * @param title The meeting title
   * @param description The meeting description
   * @param startTime The meeting start time
   * @param endTime The meeting end time
   * @param location The meeting location
   * @param calendarId The calendar ID
   * @return A MeetingDto
   */
  public static MeetingDto createMeetingDto(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UUID calendarId) {
    return MeetingDto.builder()
        .id(id)
        .title(title)
        .description(description)
        .startTime(startTime)
        .endTime(endTime)
        .location(location)
        .calendarId(calendarId)
        .build();
  }

  /**
   * Creates a MeetingDto with default values for testing.
   *
   * @param calendarId The calendar ID
   * @return A MeetingDto
   */
  public static MeetingDto createDefaultMeetingDto(UUID calendarId) {
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    return createMeetingDto(
        null,
        "Test Meeting",
        "Test Description",
        now,
        now.plusHours(1),
        "Test Location",
        calendarId);
  }

  /**
   * Creates a UserCalendar entity with the given parameters.
   *
   * @param id The user calendar ID
   * @param calendarId The calendar ID
   * @param userId The user ID
   * @return A UserCalendar entity
   */
  public static UserCalendar createUserCalendar(UUID id, UUID calendarId, UUID userId) {
    return UserCalendar.builder().id(id).calendarId(calendarId).userId(userId).build();
  }

  /**
   * Creates a UserCalendar entity with default values for testing.
   *
   * @param userId The user ID
   * @return A UserCalendar entity
   */
  public static UserCalendar createDefaultUserCalendar(UUID userId) {
    return createUserCalendar(null, UUID.randomUUID(), userId);
  }

  /**
   * Creates a TimeSlotDto with the given parameters.
   *
   * @param startTime The start time
   * @param endTime The end time
   * @param durationMinutes The duration in minutes
   * @return A TimeSlotDto
   */
  public static TimeSlotDto createTimeSlotDto(
      LocalDateTime startTime, LocalDateTime endTime, int durationMinutes) {
    return TimeSlotDto.builder()
        .startTime(startTime)
        .endTime(endTime)
        .durationMinutes(durationMinutes)
        .build();
  }

  /**
   * Creates a list of Meeting entities for testing.
   *
   * @param count The number of meetings to create
   * @param userCalendar The user calendar
   * @param calendarId The calendar ID
   * @return A list of Meeting entities
   */
  public static List<Meeting> createMeetingList(
      int count, UserCalendar userCalendar, UUID calendarId) {
    List<Meeting> meetings = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    for (int i = 0; i < count; i++) {
      meetings.add(
          createMeeting(
              UUID.randomUUID(),
              "Meeting " + i,
              "Description " + i,
              now.plusHours(i * 2L),
              now.plusHours(i * 2L + 1),
              "Location " + i,
              userCalendar,
              calendarId,
              0L));
    }
    return meetings;
  }

  /**
   * Creates a list of MeetingDto objects for testing.
   *
   * @param count The number of meeting DTOs to create
   * @param calendarId The calendar ID
   * @return A list of MeetingDto objects
   */
  public static List<MeetingDto> createMeetingDtoList(int count, UUID calendarId) {
    List<MeetingDto> meetingDtos = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    for (int i = 0; i < count; i++) {
      meetingDtos.add(
          createMeetingDto(
              UUID.randomUUID(),
              "Meeting " + i,
              "Description " + i,
              now.plusHours(i * 2L),
              now.plusHours(i * 2L + 1),
              "Location " + i,
              calendarId));
    }
    return meetingDtos;
  }

  /**
   * Creates a list of TimeSlotDto objects for testing.
   *
   * @param count The number of time slot DTOs to create
   * @param durationMinutes The duration in minutes
   * @return A list of TimeSlotDto objects
   */
  public static List<TimeSlotDto> createTimeSlotDtoList(int count, int durationMinutes) {
    List<TimeSlotDto> timeSlotDtos = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    for (int i = 0; i < count; i++) {
      LocalDateTime startTime = now.plusHours(i * 2L);
      LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
      timeSlotDtos.add(createTimeSlotDto(startTime, endTime, durationMinutes));
    }
    return timeSlotDtos;
  }
}
