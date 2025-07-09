package doodle.qa.com.svcproviderqa.unit.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcproviderqa.controller.EventController;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.exception.EventNotFoundException;
import doodle.qa.com.svcproviderqa.service.EventService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for the EventController. These tests verify the REST API endpoints using MockMvc. */
@WebMvcTest(EventController.class)
class EventControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private EventService eventService;

  @Test
  @DisplayName("Should return all events when getting all events without explicit pagination")
  void getAllEvents_WithDefaultPagination_ShouldReturnPagedEvents() throws Exception {
    // Given
    List<EventDto> events = TestDataFactory.createEventDtoList(3, UUID.randomUUID());
    Page<EventDto> eventPage = new PageImpl<>(events, PageRequest.of(0, 20), events.size());

    // Mock the service to return the page when called with any Pageable
    when(eventService.getAllEvents(any(Pageable.class))).thenReturn(eventPage);

    // When/Then
    mockMvc
        .perform(get("/api/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.events", hasSize(3)))
        .andExpect(jsonPath("$.events[0].title", is(events.get(0).getTitle())))
        .andExpect(jsonPath("$.events[1].title", is(events.get(1).getTitle())))
        .andExpect(jsonPath("$.events[2].title", is(events.get(2).getTitle())))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(0)));

    verify(eventService).getAllEvents(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return paginated events when getting events with pagination parameters")
  void getAllEvents_WithPagination_ShouldReturnPagedEvents() throws Exception {
    // Given
    List<EventDto> events = TestDataFactory.createEventDtoList(5, UUID.randomUUID());
    int pageSize = 2;
    int pageNumber = 1; // Second page (0-indexed)

    // Create a Page object with the events
    Page<EventDto> eventPage =
        new PageImpl<>(
            events.subList(
                pageNumber * pageSize, Math.min((pageNumber + 1) * pageSize, events.size())),
            PageRequest.of(pageNumber, pageSize),
            events.size());

    // Mock the service to return the page when called with the pageable parameter
    when(eventService.getAllEvents(any(Pageable.class))).thenReturn(eventPage);

    // When/Then
    mockMvc
        .perform(
            get("/api/events")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.events", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is((int) Math.ceil((double) events.size() / pageSize))));

    verify(eventService).getAllEvents(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return event when getting event by ID that exists")
  void getEventById_WhenEventExists_ShouldReturnEvent() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto event = TestDataFactory.createEventDto(
        eventId, "Test Event", "Test Description", now, now.plusHours(1), "Test Location", calendarId);
    when(eventService.getEventById(eventId)).thenReturn(event);

    // When/Then
    mockMvc
        .perform(get("/api/events/{id}", eventId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(eventId.toString())))
        .andExpect(jsonPath("$.title", is("Test Event")))
        .andExpect(jsonPath("$.description", is("Test Description")))
        .andExpect(jsonPath("$.calendarId", is(calendarId.toString())));

    verify(eventService).getEventById(eventId);
  }

  @Test
  @DisplayName("Should return 404 when getting event by ID that doesn't exist")
  void getEventById_WhenEventDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    when(eventService.getEventById(eventId)).thenThrow(new EventNotFoundException(eventId));

    // When/Then
    mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isNotFound());

    verify(eventService).getEventById(eventId);
  }

  @Test
  @DisplayName("Should return events when getting events by calendar ID")
  void getEventsByCalendarId_ShouldReturnEvents() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    List<EventDto> events = TestDataFactory.createEventDtoList(3, calendarId);
    when(eventService.getEventsByCalendarId(calendarId)).thenReturn(events);

    // When/Then
    mockMvc
        .perform(get("/api/events/calendar/{calendarId}", calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].title", is(events.get(0).getTitle())))
        .andExpect(jsonPath("$[1].title", is(events.get(1).getTitle())))
        .andExpect(jsonPath("$[2].title", is(events.get(2).getTitle())));

    verify(eventService).getEventsByCalendarId(calendarId);
  }

  @Test
  @DisplayName("Should return events when getting events by time range")
  void getEventsByTimeRange_ShouldReturnEvents() throws Exception {
    // Given
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusDays(7);
    List<EventDto> events = TestDataFactory.createEventDtoList(3, UUID.randomUUID());
    when(eventService.getEventsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(events);

    // Format dates for URL
    String startParam = start.format(DateTimeFormatter.ISO_DATE_TIME);
    String endParam = end.format(DateTimeFormatter.ISO_DATE_TIME);

    // When/Then
    mockMvc
        .perform(get("/api/events/timerange")
            .param("start", startParam)
            .param("end", endParam))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].title", is(events.get(0).getTitle())))
        .andExpect(jsonPath("$[1].title", is(events.get(1).getTitle())))
        .andExpect(jsonPath("$[2].title", is(events.get(2).getTitle())));

    verify(eventService).getEventsByTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should return events when getting events by calendar ID and time range")
  void getEventsByCalendarIdAndTimeRange_ShouldReturnEvents() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = start.plusDays(7);
    List<EventDto> events = TestDataFactory.createEventDtoList(3, calendarId);
    when(eventService.getEventsByCalendarIdAndTimeRange(
        eq(calendarId), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(events);

    // Format dates for URL
    String startParam = start.format(DateTimeFormatter.ISO_DATE_TIME);
    String endParam = end.format(DateTimeFormatter.ISO_DATE_TIME);

    // When/Then
    mockMvc
        .perform(get("/api/events/calendar/{calendarId}/timerange", calendarId)
            .param("start", startParam)
            .param("end", endParam))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].title", is(events.get(0).getTitle())))
        .andExpect(jsonPath("$[1].title", is(events.get(1).getTitle())))
        .andExpect(jsonPath("$[2].title", is(events.get(2).getTitle())));

    verify(eventService).getEventsByCalendarIdAndTimeRange(
        eq(calendarId), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("Should create event when creating event with valid data")
  void createEvent_WithValidData_ShouldCreateEvent() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventToCreate = TestDataFactory.createEventDto(
        "New Event", "New Description", now, now.plusHours(1), "New Location", calendarId);
    EventDto createdEvent = TestDataFactory.createEventDto(
        UUID.randomUUID(), "New Event", "New Description", now, now.plusHours(1), "New Location", calendarId);
    when(eventService.createEvent(any(EventDto.class))).thenReturn(createdEvent);

    // When/Then
    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventToCreate)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(createdEvent.getId().toString())))
        .andExpect(jsonPath("$.title", is("New Event")))
        .andExpect(jsonPath("$.description", is("New Description")))
        .andExpect(jsonPath("$.calendarId", is(calendarId.toString())));

    verify(eventService).createEvent(any(EventDto.class));
  }

  @Test
  @DisplayName("Should return 400 when creating event with invalid data")
  void createEvent_WithInvalidData_ShouldReturn400() throws Exception {
    // Given
    EventDto invalidEvent = EventDto.builder()
        .description("Invalid Event")
        .calendarId(UUID.randomUUID())
        .build(); // Missing title, start time, and end time

    // When/Then
    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEvent)))
        .andExpect(status().isBadRequest());

    verify(eventService, never()).createEvent(any(EventDto.class));
  }

  @Test
  @DisplayName("Should return 404 when creating event with non-existent calendar")
  void createEvent_WithNonExistentCalendar_ShouldReturn404() throws Exception {
    // Given
    UUID nonExistentCalendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventToCreate = TestDataFactory.createEventDto(
        "New Event", "New Description", now, now.plusHours(1), "New Location", nonExistentCalendarId);
    when(eventService.createEvent(any(EventDto.class)))
        .thenThrow(new CalendarNotFoundException(nonExistentCalendarId));

    // When/Then
    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventToCreate)))
        .andExpect(status().isNotFound());

    verify(eventService).createEvent(any(EventDto.class));
  }

  @Test
  @DisplayName("Should update event when updating event that exists with valid data")
  void updateEvent_WhenEventExistsWithValidData_ShouldUpdateEvent() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventToUpdate = TestDataFactory.createEventDto(
        eventId, "Updated Event", "Updated Description", now.plusHours(2), now.plusHours(3),
        "Updated Location", calendarId);
    when(eventService.updateEvent(eq(eventId), any(EventDto.class))).thenReturn(eventToUpdate);

    // When/Then
    mockMvc
        .perform(
            put("/api/events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventToUpdate)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(eventId.toString())))
        .andExpect(jsonPath("$.title", is("Updated Event")))
        .andExpect(jsonPath("$.description", is("Updated Description")))
        .andExpect(jsonPath("$.location", is("Updated Location")))
        .andExpect(jsonPath("$.calendarId", is(calendarId.toString())));

    verify(eventService).updateEvent(eq(eventId), any(EventDto.class));
  }

  @Test
  @DisplayName("Should return 404 when updating event that doesn't exist")
  void updateEvent_WhenEventDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventToUpdate = TestDataFactory.createEventDto(
        eventId, "Updated Event", "Updated Description", now.plusHours(2), now.plusHours(3),
        "Updated Location", calendarId);
    when(eventService.updateEvent(eq(eventId), any(EventDto.class)))
        .thenThrow(new EventNotFoundException("Event not found with id: " + eventId));

    // When/Then
    mockMvc
        .perform(
            put("/api/events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventToUpdate)))
        .andExpect(status().isNotFound());

    verify(eventService).updateEvent(eq(eventId), any(EventDto.class));
  }

  @Test
  @DisplayName("Should delete event when deleting event that exists")
  void deleteEvent_WhenEventExists_ShouldDeleteEvent() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    doNothing().when(eventService).deleteEvent(eventId);

    // When/Then
    mockMvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNoContent());

    verify(eventService).deleteEvent(eventId);
  }

  @Test
  @DisplayName("Should return 404 when deleting event that doesn't exist")
  void deleteEvent_WhenEventDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    doThrow(new EventNotFoundException("Event not found with id: " + eventId))
        .when(eventService)
        .deleteEvent(eventId);

    // When/Then
    mockMvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNotFound());

    verify(eventService).deleteEvent(eventId);
  }
}