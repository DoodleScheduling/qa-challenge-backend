package doodle.qa.com.svcproviderqa.integration.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.util.UUID;
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
 * Integration tests for the CalendarController. These tests verify the REST API endpoints with the
 * actual service implementation, ensuring that the controller correctly interacts with the service
 * layer and returns appropriate responses for various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalendarControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CalendarService calendarService;

  @Test
  @DisplayName(
      "Should create a calendar and then retrieve it by ID and in the list of all calendars")
  void testCreateAndRetrieveCalendar() throws Exception {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Test Calendar", "Test Description");
    String calendarJson = objectMapper.writeValueAsString(calendarDto);

    // When/Then - Create calendar
    String responseJson =
        mockMvc
            .perform(
                post("/api/calendars")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(calendarJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Test Calendar")))
            .andExpect(jsonPath("$.description", is("Test Description")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract ID from response
    CalendarDto createdCalendar = objectMapper.readValue(responseJson, CalendarDto.class);
    UUID calendarId = createdCalendar.getId();

    // When/Then - Get calendar by ID
    mockMvc
        .perform(get("/api/calendars/{id}", calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(calendarId.toString())))
        .andExpect(jsonPath("$.name", is("Test Calendar")))
        .andExpect(jsonPath("$.description", is("Test Description")));

    // When/Then - Get all calendars with pagination
    mockMvc
        .perform(get("/api/calendars"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(1)))
        .andExpect(jsonPath("$.calendars[0].id", is(calendarId.toString())))
        .andExpect(jsonPath("$.totalPages").exists())
        .andExpect(jsonPath("$.currentPage").exists());
  }

  @Test
  @DisplayName("Should update a calendar's information")
  void testUpdateCalendar() throws Exception {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Original Calendar", "Original Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    UUID calendarId = createdCalendar.getId();

    CalendarDto updateDto =
        TestDataFactory.createCalendarDto(
            calendarId, "Updated Calendar", "Updated Description", null);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    // When/Then
    mockMvc
        .perform(
            put("/api/calendars/{id}", calendarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(calendarId.toString())))
        .andExpect(jsonPath("$.name", is("Updated Calendar")))
        .andExpect(jsonPath("$.description", is("Updated Description")));
  }

  @Test
  @DisplayName("Should delete a calendar")
  void testDeleteCalendar() throws Exception {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Calendar To Delete", "Delete Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    UUID calendarId = createdCalendar.getId();

    // When/Then - Delete calendar
    mockMvc.perform(delete("/api/calendars/{id}", calendarId)).andExpect(status().isNoContent());

    // Verify calendar is deleted
    mockMvc.perform(get("/api/calendars/{id}", calendarId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 404 when calendar is not found")
  void testCalendarNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    // When/Then - Get non-existent calendar
    mockMvc.perform(get("/api/calendars/{id}", nonExistentId)).andExpect(status().isNotFound());

    // When/Then - Update non-existent calendar
    CalendarDto updateDto =
        TestDataFactory.createCalendarDto(
            nonExistentId, "Updated Calendar", "Updated Description", null);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    mockMvc
        .perform(
            put("/api/calendars/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isNotFound());

    // When/Then - Delete non-existent calendar
    mockMvc.perform(delete("/api/calendars/{id}", nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when creating a calendar with invalid data")
  void testCreateCalendarWithInvalidData() throws Exception {
    // Given
    CalendarDto invalidCalendar =
        CalendarDto.builder().description("Invalid Calendar").build(); // Missing name
    String invalidJson = objectMapper.writeValueAsString(invalidCalendar);

    // When/Then
    mockMvc
        .perform(
            post("/api/calendars").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should retrieve calendars with explicit pagination parameters")
  void testGetCalendarsWithPagination() throws Exception {
    // Given
    int totalCalendars = 15;
    int pageSize = 5;
    int pageNumber = 1; // Second page (0-indexed)

    // Create multiple calendars
    for (int i = 0; i < totalCalendars; i++) {
      calendarService.createCalendar(
          TestDataFactory.createCalendarDto("Calendar " + i, "Description " + i));
    }

    // When/Then - Get calendars with pagination
    mockMvc
        .perform(
            get("/api/calendars")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(
            jsonPath("$.totalPages", is((int) Math.ceil((double) totalCalendars / pageSize))));
  }

  @Test
  @DisplayName("Should handle empty page gracefully")
  void testGetCalendarsEmptyPage() throws Exception {
    // Given - No calendars in the database

    // When/Then - Get calendars with pagination
    mockMvc
        .perform(get("/api/calendars"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(0)))
        .andExpect(jsonPath("$.totalPages", is(0)))
        .andExpect(jsonPath("$.currentPage", is(0)));
  }

  @Test
  @DisplayName("Should handle page number out of bounds gracefully")
  void testGetCalendarsPageOutOfBounds() throws Exception {
    // Given
    int totalCalendars = 5;

    // Create some calendars
    for (int i = 0; i < totalCalendars; i++) {
      calendarService.createCalendar(
          TestDataFactory.createCalendarDto("Calendar " + i, "Description " + i));
    }

    // When/Then - Request a page that is out of bounds
    mockMvc
        .perform(
            get("/api/calendars")
                .param("page", "10") // Page that doesn't exist
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(0)))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(10)));
  }
}
