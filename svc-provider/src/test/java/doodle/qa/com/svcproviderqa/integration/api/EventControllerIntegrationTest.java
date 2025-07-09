package doodle.qa.com.svcproviderqa.integration.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.service.EventService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the EventController. These tests verify the REST API endpoints with the
 * actual service implementation, ensuring that the controller correctly interacts with the service
 * layer and returns appropriate responses for various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private EventService eventService;

  @Autowired private CalendarService calendarService;

  private UUID calendarId;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    // Create a calendar for testing
    CalendarDto calendarDto = TestDataFactory.createCalendarDto("Test Calendar", "Test Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    calendarId = createdCalendar.getId();
    now = LocalDateTime.now();
  }

  @Test
  @DisplayName("Should create an event and then retrieve it by ID and in the list of all events")
  void testCreateAndRetrieveEvent() throws Exception {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Test Event", "Test Description", now, now.plusHours(1), "Test Location", calendarId);
    String eventJson = objectMapper.writeValueAsString(eventDto);

    // When/Then - Create event
    String responseJson =
        mockMvc
            .perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(eventJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title", is("Test Event")))
            .andExpect(jsonPath("$.description", is("Test Description")))
            .andExpect(jsonPath("$.calendarId", is(calendarId.toString())))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract ID from response
    EventDto createdEvent = objectMapper.readValue(responseJson, EventDto.class);
    UUID eventId = createdEvent.getId();

    // When/Then - Get event by ID
    mockMvc
        .perform(get("/api/events/{id}", eventId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(eventId.toString())))
        .andExpect(jsonPath("$.title", is("Test Event")))
        .andExpect(jsonPath("$.description", is("Test Description")))
        .andExpect(jsonPath("$.calendarId", is(calendarId.toString())));

    // When/Then - Get all events with pagination
    mockMvc
        .perform(get("/api/events"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.events", hasSize(1)))
        .andExpect(jsonPath("$.events[0].id", is(eventId.toString())))
        .andExpect(jsonPath("$.totalPages").exists())
        .andExpect(jsonPath("$.currentPage").exists());
  }

  @Test
  @DisplayName("Should update an event's information")
  void testUpdateEvent() throws Exception {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Original Event", "Original Description", now, now.plusHours(1), "Original Location", calendarId);
    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    EventDto updateDto = TestDataFactory.createEventDto(
        eventId, "Updated Event", "Updated Description", now.plusHours(2), now.plusHours(3),
        "Updated Location", calendarId);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    // When/Then
    mockMvc
        .perform(
            put("/api/events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(eventId.toString())))
        .andExpect(jsonPath("$.title", is("Updated Event")))
        .andExpect(jsonPath("$.description", is("Updated Description")))
        .andExpect(jsonPath("$.location", is("Updated Location")));
  }

  @Test
  @DisplayName("Should delete an event")
  void testDeleteEvent() throws Exception {
    // Given
    EventDto eventDto = TestDataFactory.createEventDto(
        "Event To Delete", "Delete Description", now, now.plusHours(1), "Delete Location", calendarId);
    EventDto createdEvent = eventService.createEvent(eventDto);
    UUID eventId = createdEvent.getId();

    // When/Then - Delete event
    mockMvc.perform(delete("/api/events/{id}", eventId)).andExpect(status().isNoContent());

    // Verify event is deleted
    mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should retrieve events by calendar ID")
  void testGetEventsByCalendarId() throws Exception {
    // Given
    EventDto event1 = TestDataFactory.createEventDto(
        "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendarId);
    EventDto event2 = TestDataFactory.createEventDto(
        "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendarId);
    
    eventService.createEvent(event1);
    eventService.createEvent(event2);

    // When/Then
    mockMvc
        .perform(get("/api/events/calendar/{calendarId}", calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("Event 1")))
        .andExpect(jsonPath("$[1].title", is("Event 2")));
  }

  @Test
  @DisplayName("Should retrieve events by time range")
  void testGetEventsByTimeRange() throws Exception {
    // Given
    EventDto event1 = TestDataFactory.createEventDto(
        "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendarId);
    EventDto event2 = TestDataFactory.createEventDto(
        "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendarId);
    EventDto event3 = TestDataFactory.createEventDto(
        "Event 3", "Description 3", now.plusHours(4), now.plusHours(5), "Location 3", calendarId);
    
    eventService.createEvent(event1);
    eventService.createEvent(event2);
    eventService.createEvent(event3);

    // Format dates for URL
    String startTime = now.minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME);
    String endTime = now.plusHours(3).format(DateTimeFormatter.ISO_DATE_TIME);

    // When/Then
    mockMvc
        .perform(get("/api/events/timerange")
            .param("start", startTime)
            .param("end", endTime))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("Event 1")))
        .andExpect(jsonPath("$[1].title", is("Event 2")));
  }

  @Test
  @DisplayName("Should retrieve events by calendar ID and time range")
  void testGetEventsByCalendarIdAndTimeRange() throws Exception {
    // Given
    // Create another calendar
    CalendarDto anotherCalendarDto = TestDataFactory.createCalendarDto("Another Calendar", "Another Description");
    CalendarDto anotherCalendar = calendarService.createCalendar(anotherCalendarDto);
    UUID anotherCalendarId = anotherCalendar.getId();
    
    EventDto event1 = TestDataFactory.createEventDto(
        "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendarId);
    EventDto event2 = TestDataFactory.createEventDto(
        "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendarId);
    EventDto event3 = TestDataFactory.createEventDto(
        "Event 3", "Description 3", now.plusHours(1), now.plusHours(2), "Location 3", anotherCalendarId);
    
    eventService.createEvent(event1);
    eventService.createEvent(event2);
    eventService.createEvent(event3);

    // Format dates for URL
    String startTime = now.minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME);
    String endTime = now.plusHours(3).format(DateTimeFormatter.ISO_DATE_TIME);

    // When/Then
    mockMvc
        .perform(get("/api/events/calendar/{calendarId}/timerange", calendarId)
            .param("start", startTime)
            .param("end", endTime))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("Event 1")))
        .andExpect(jsonPath("$[1].title", is("Event 2")));
  }

  @Test
  @DisplayName("Should return 404 when event is not found")
  void testEventNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    // When/Then - Get non-existent event
    mockMvc.perform(get("/api/events/{id}", nonExistentId)).andExpect(status().isNotFound());

    // When/Then - Update non-existent event
    EventDto updateDto = TestDataFactory.createEventDto(
        nonExistentId, "Updated Event", "Updated Description", now, now.plusHours(1),
        "Updated Location", calendarId);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    mockMvc
        .perform(
            put("/api/events/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isNotFound());

    // When/Then - Delete non-existent event
    mockMvc.perform(delete("/api/events/{id}", nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when calendar is not found")
  void testCalendarNotFound() throws Exception {
    // Given
    UUID nonExistentCalendarId = UUID.randomUUID();
    EventDto eventDto = TestDataFactory.createEventDto(
        "Test Event", "Test Description", now, now.plusHours(1), "Test Location", nonExistentCalendarId);
    String eventJson = objectMapper.writeValueAsString(eventDto);

    // When/Then - Create event with non-existent calendar
    mockMvc
        .perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(eventJson))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when creating an event with invalid data")
  void testCreateEventWithInvalidData() throws Exception {
    // Given
    EventDto invalidEvent = EventDto.builder()
        .description("Invalid Event")
        .calendarId(calendarId)
        .build(); // Missing title, start time, and end time
    String invalidJson = objectMapper.writeValueAsString(invalidEvent);

    // When/Then
    mockMvc
        .perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should retrieve events with explicit pagination parameters")
  void testGetEventsWithPagination() throws Exception {
    // Given
    int totalEvents = 15;
    int pageSize = 5;
    int pageNumber = 1; // Second page (0-indexed)

    // Create multiple events
    for (int i = 0; i < totalEvents; i++) {
      eventService.createEvent(
          TestDataFactory.createEventDto(
              "Event " + i, "Description " + i, now.plusHours(i), now.plusHours(i + 1),
              "Location " + i, calendarId));
    }

    // When/Then - Get events with pagination
    mockMvc
        .perform(
            get("/api/events")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.events", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is((int) Math.ceil((double) totalEvents / pageSize))));
  }
}