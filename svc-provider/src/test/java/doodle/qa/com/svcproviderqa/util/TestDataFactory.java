package doodle.qa.com.svcproviderqa.util;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.entity.Event;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects. This class provides methods to create test
 * calendars and events with predefined or random data.
 */
public class TestDataFactory {

  /**
   * Creates a Calendar entity with the given ID, name, description, events, and version.
   *
   * @param id The calendar ID
   * @param name The calendar name
   * @param description The calendar description
   * @param events The list of events
   * @param version The version for optimistic locking
   * @return A Calendar entity
   */
  public static Calendar createCalendar(
      UUID id, String name, String description, List<Event> events, Long version) {
    Calendar calendar =
        Calendar.builder().id(id).name(name).description(description).version(version).build();

    if (events != null) {
      events.forEach(calendar::addEvent);
    }

    return calendar;
  }

  /**
   * Creates a Calendar entity with the given ID, name, description, and events. The version will be
   * null, which is appropriate for new calendars that haven't been persisted yet.
   *
   * @param id The calendar ID
   * @param name The calendar name
   * @param description The calendar description
   * @param events The list of events
   * @return A Calendar entity
   */
  public static Calendar createCalendar(
      UUID id, String name, String description, List<Event> events) {
    return createCalendar(id, name, description, events, null);
  }

  /**
   * Creates a Calendar entity with the given name, description, and events. The ID will be null,
   * which is appropriate for new calendars that haven't been persisted yet.
   *
   * @param name The calendar name
   * @param description The calendar description
   * @param events The list of events
   * @return A Calendar entity
   */
  public static Calendar createCalendar(String name, String description, List<Event> events) {
    return createCalendar(null, name, description, events);
  }

  /**
   * Creates a Calendar entity with the given name and description. The ID will be null and the
   * events list will be empty.
   *
   * @param name The calendar name
   * @param description The calendar description
   * @return A Calendar entity
   */
  public static Calendar createCalendar(String name, String description) {
    return createCalendar(null, name, description, new ArrayList<>());
  }

  /**
   * Creates a CalendarDto with the given ID, name, description, and version.
   *
   * @param id The calendar ID
   * @param name The calendar name
   * @param description The calendar description
   * @param version The version for optimistic locking
   * @return A CalendarDto
   */
  public static CalendarDto createCalendarDto(
      UUID id, String name, String description, Long version) {
    return CalendarDto.builder()
        .id(id)
        .name(name)
        .description(description)
        .version(version)
        .build();
  }

  /**
   * Creates a CalendarDto with the given ID, name, and description. The version will be null, which
   * is appropriate for new calendars that haven't been persisted yet.
   *
   * @param id The calendar ID
   * @param name The calendar name
   * @param description The calendar description
   * @return A CalendarDto
   */
  public static CalendarDto createCalendarDto(UUID id, String name, String description) {
    return createCalendarDto(id, name, description, null);
  }

  /**
   * Creates a CalendarDto with the given name, description, and version. The ID will be null, which
   * is appropriate for new calendars that haven't been persisted yet.
   *
   * @param name The calendar name
   * @param description The calendar description
   * @param version The version for optimistic locking
   * @return A CalendarDto
   */
  public static CalendarDto createCalendarDto(String name, String description, Long version) {
    return createCalendarDto(null, name, description, version);
  }

  /**
   * Creates a CalendarDto with the given name and description. The ID and version will be null,
   * which is appropriate for new calendars that haven't been persisted yet.
   *
   * @param name The calendar name
   * @param description The calendar description
   * @return A CalendarDto
   */
  public static CalendarDto createCalendarDto(String name, String description) {
    return createCalendarDto(null, name, description, null);
  }

  /**
   * Creates an Event entity with the given ID, title, description, start time, end time, location,
   * calendar, and version.
   *
   * @param id The event ID
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendar The calendar the event belongs to
   * @param version The version for optimistic locking
   * @return An Event entity
   */
  public static Event createEvent(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      Calendar calendar,
      Long version) {
    return Event.builder()
        .id(id)
        .title(title)
        .description(description)
        .startTime(startTime)
        .endTime(endTime)
        .location(location)
        .calendar(calendar)
        .version(version)
        .build();
  }

  /**
   * Creates an Event entity with the given ID, title, description, start time, end time, location,
   * and calendar. The version will be null, which is appropriate for new events that haven't been
   * persisted yet.
   *
   * @param id The event ID
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendar The calendar the event belongs to
   * @return An Event entity
   */
  public static Event createEvent(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      Calendar calendar) {
    return createEvent(id, title, description, startTime, endTime, location, calendar, null);
  }

  /**
   * Creates an Event entity with the given title, description, start time, end time, location, and
   * calendar. The ID will be null, which is appropriate for new events that haven't been persisted
   * yet.
   *
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendar The calendar the event belongs to
   * @return An Event entity
   */
  public static Event createEvent(
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      Calendar calendar) {
    return createEvent(null, title, description, startTime, endTime, location, calendar);
  }

  /**
   * Creates an EventDto with the given ID, title, description, start time, end time, location,
   * calendar ID, and version.
   *
   * @param id The event ID
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendarId The ID of the calendar the event belongs to
   * @param version The version for optimistic locking
   * @return An EventDto
   */
  public static EventDto createEventDto(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UUID calendarId,
      Long version) {
    return EventDto.builder()
        .id(id)
        .title(title)
        .description(description)
        .startTime(startTime)
        .endTime(endTime)
        .location(location)
        .calendarId(calendarId)
        .version(version)
        .build();
  }

  /**
   * Creates an EventDto with the given ID, title, description, start time, end time, location, and
   * calendar ID. The version will be null, which is appropriate for new events that haven't been
   * persisted yet.
   *
   * @param id The event ID
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendarId The ID of the calendar the event belongs to
   * @return An EventDto
   */
  public static EventDto createEventDto(
      UUID id,
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UUID calendarId) {
    return createEventDto(id, title, description, startTime, endTime, location, calendarId, null);
  }

  /**
   * Creates an EventDto with the given title, description, start time, end time, location, and
   * calendar ID. The ID will be null, which is appropriate for new events that haven't been
   * persisted yet.
   *
   * @param title The event title
   * @param description The event description
   * @param startTime The event start time
   * @param endTime The event end time
   * @param location The event location
   * @param calendarId The ID of the calendar the event belongs to
   * @return An EventDto
   */
  public static EventDto createEventDto(
      String title,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String location,
      UUID calendarId) {
    return createEventDto(null, title, description, startTime, endTime, location, calendarId);
  }

  /**
   * Creates a list of Calendar entities for testing with version information.
   *
   * @param count The number of calendars to create
   * @return A list of Calendar entities
   */
  public static List<Calendar> createCalendarList(int count) {
    List<Calendar> calendars = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      calendars.add(
          createCalendar(
              UUID.randomUUID(),
              "Calendar " + i,
              "Description for calendar " + i,
              new ArrayList<>(),
              0L // Initialize with version 0
              ));
    }
    return calendars;
  }

  /**
   * Creates a list of CalendarDto objects for testing with version information.
   *
   * @param count The number of calendar DTOs to create
   * @return A list of CalendarDto objects
   */
  public static List<CalendarDto> createCalendarDtoList(int count) {
    List<CalendarDto> calendarDtos = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      calendarDtos.add(
          createCalendarDto(
              UUID.randomUUID(),
              "Calendar " + i,
              "Description for calendar " + i,
              0L // Initialize with version 0
              ));
    }
    return calendarDtos;
  }

  /**
   * Creates a list of Event entities for testing with version information.
   *
   * @param count The number of events to create
   * @param calendar The calendar to associate with the events
   * @return A list of Event entities
   */
  public static List<Event> createEventList(int count, Calendar calendar) {
    List<Event> events = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i < count; i++) {
      events.add(
          createEvent(
              UUID.randomUUID(),
              "Event " + i,
              "Description for event " + i,
              now.plusHours(i),
              now.plusHours(i + 1),
              "Location " + i,
              calendar,
              0L // Initialize with version 0
              ));
    }
    return events;
  }

  /**
   * Creates a list of EventDto objects for testing with version information.
   *
   * @param count The number of event DTOs to create
   * @param calendarId The ID of the calendar to associate with the events
   * @return A list of EventDto objects
   */
  public static List<EventDto> createEventDtoList(int count, UUID calendarId) {
    List<EventDto> eventDtos = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i < count; i++) {
      eventDtos.add(
          createEventDto(
              UUID.randomUUID(),
              "Event " + i,
              "Description for event " + i,
              now.plusHours(i),
              now.plusHours(i + 1),
              "Location " + i,
              calendarId,
              0L // Initialize with version 0
              ));
    }
    return eventDtos;
  }
}
