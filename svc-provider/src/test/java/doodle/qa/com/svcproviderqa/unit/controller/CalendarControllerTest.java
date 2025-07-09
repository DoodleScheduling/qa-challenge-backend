package doodle.qa.com.svcproviderqa.unit.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcproviderqa.controller.CalendarController;
import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
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

/** Unit tests for the CalendarController. These tests verify the REST API endpoints using MockMvc. */
@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CalendarService calendarService;

  @Test
  @DisplayName("Should return all calendars when getting all calendars without explicit pagination")
  void getAllCalendars_WithDefaultPagination_ShouldReturnPagedCalendars() throws Exception {
    // Given
    List<CalendarDto> calendars = TestDataFactory.createCalendarDtoList(3);
    Page<CalendarDto> calendarPage = new PageImpl<>(calendars, PageRequest.of(0, 20), calendars.size());

    // Mock the service to return the page when called with any Pageable
    when(calendarService.getAllCalendars(any(Pageable.class))).thenReturn(calendarPage);

    // When/Then
    mockMvc
        .perform(get("/api/calendars"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(3)))
        .andExpect(jsonPath("$.calendars[0].name", is(calendars.get(0).getName())))
        .andExpect(jsonPath("$.calendars[1].name", is(calendars.get(1).getName())))
        .andExpect(jsonPath("$.calendars[2].name", is(calendars.get(2).getName())))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(0)));

    verify(calendarService).getAllCalendars(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return paginated calendars when getting calendars with pagination parameters")
  void getAllCalendars_WithPagination_ShouldReturnPagedCalendars() throws Exception {
    // Given
    List<CalendarDto> calendars = TestDataFactory.createCalendarDtoList(5);
    int pageSize = 2;
    int pageNumber = 1; // Second page (0-indexed)

    // Create a Page object with the calendars
    Page<CalendarDto> calendarPage =
        new PageImpl<>(
            calendars.subList(
                pageNumber * pageSize, Math.min((pageNumber + 1) * pageSize, calendars.size())),
            PageRequest.of(pageNumber, pageSize),
            calendars.size());

    // Mock the service to return the page when called with the pageable parameter
    when(calendarService.getAllCalendars(any(Pageable.class))).thenReturn(calendarPage);

    // When/Then
    mockMvc
        .perform(
            get("/api/calendars")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is((int) Math.ceil((double) calendars.size() / pageSize))));

    verify(calendarService).getAllCalendars(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page when getting calendars with page number out of bounds")
  void getAllCalendars_WithPageOutOfBounds_ShouldReturnEmptyPage() throws Exception {
    // Given
    List<CalendarDto> calendars = TestDataFactory.createCalendarDtoList(5);
    int pageSize = 10;
    int pageNumber = 10; // Page that doesn't exist

    // Create an empty Page object
    Page<CalendarDto> emptyPage =
        new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), calendars.size());

    // Mock the service to return the empty page
    when(calendarService.getAllCalendars(any(Pageable.class))).thenReturn(emptyPage);

    // When/Then
    mockMvc
        .perform(
            get("/api/calendars")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.calendars", hasSize(0)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is(1)));

    verify(calendarService).getAllCalendars(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return calendar when getting calendar by ID that exists")
  void getCalendarById_WhenCalendarExists_ShouldReturnCalendar() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendar = TestDataFactory.createCalendarDto(calendarId, "Test Calendar", "Test Description", null);
    when(calendarService.getCalendarById(calendarId)).thenReturn(calendar);

    // When/Then
    mockMvc
        .perform(get("/api/calendars/{id}", calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(calendarId.toString())))
        .andExpect(jsonPath("$.name", is("Test Calendar")))
        .andExpect(jsonPath("$.description", is("Test Description")));

    verify(calendarService).getCalendarById(calendarId);
  }

  @Test
  @DisplayName("Should return 404 when getting calendar by ID that doesn't exist")
  void getCalendarById_WhenCalendarDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    when(calendarService.getCalendarById(calendarId)).thenThrow(new CalendarNotFoundException(calendarId));

    // When/Then
    mockMvc.perform(get("/api/calendars/{id}", calendarId)).andExpect(status().isNotFound());

    verify(calendarService).getCalendarById(calendarId);
  }

  @Test
  @DisplayName("Should create calendar when creating calendar with valid data")
  void createCalendar_WithValidData_ShouldCreateCalendar() throws Exception {
    // Given
    CalendarDto calendarToCreate = TestDataFactory.createCalendarDto("New Calendar", "New Description");
    CalendarDto createdCalendar =
        TestDataFactory.createCalendarDto(UUID.randomUUID(), "New Calendar", "New Description", null);
    when(calendarService.createCalendar(any(CalendarDto.class))).thenReturn(createdCalendar);

    // When/Then
    mockMvc
        .perform(
            post("/api/calendars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calendarToCreate)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(createdCalendar.getId().toString())))
        .andExpect(jsonPath("$.name", is("New Calendar")))
        .andExpect(jsonPath("$.description", is("New Description")));

    verify(calendarService).createCalendar(any(CalendarDto.class));
  }

  @Test
  @DisplayName("Should return 400 when creating calendar with invalid data")
  void createCalendar_WithInvalidData_ShouldReturn400() throws Exception {
    // Given
    CalendarDto invalidCalendar =
        CalendarDto.builder().description("Invalid Calendar").build(); // Missing name

    // When/Then
    mockMvc
        .perform(
            post("/api/calendars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCalendar)))
        .andExpect(status().isBadRequest());

    verify(calendarService, never()).createCalendar(any(CalendarDto.class));
  }

  @Test
  @DisplayName("Should update calendar when updating calendar that exists with valid data")
  void updateCalendar_WhenCalendarExistsWithValidData_ShouldUpdateCalendar() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarToUpdate =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description", null);
    when(calendarService.updateCalendar(eq(calendarId), any(CalendarDto.class))).thenReturn(calendarToUpdate);

    // When/Then
    mockMvc
        .perform(
            put("/api/calendars/{id}", calendarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calendarToUpdate)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(calendarId.toString())))
        .andExpect(jsonPath("$.name", is("Updated Calendar")))
        .andExpect(jsonPath("$.description", is("Updated Description")));

    verify(calendarService).updateCalendar(eq(calendarId), any(CalendarDto.class));
  }

  @Test
  @DisplayName("Should return 404 when updating calendar that doesn't exist")
  void updateCalendar_WhenCalendarDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarToUpdate =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description", null);
    when(calendarService.updateCalendar(eq(calendarId), any(CalendarDto.class)))
        .thenThrow(new CalendarNotFoundException("Calendar not found with id: " + calendarId));

    // When/Then
    mockMvc
        .perform(
            put("/api/calendars/{id}", calendarId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calendarToUpdate)))
        .andExpect(status().isNotFound());

    verify(calendarService).updateCalendar(eq(calendarId), any(CalendarDto.class));
  }

  @Test
  @DisplayName("Should delete calendar when deleting calendar that exists")
  void deleteCalendar_WhenCalendarExists_ShouldDeleteCalendar() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    doNothing().when(calendarService).deleteCalendar(calendarId);

    // When/Then
    mockMvc.perform(delete("/api/calendars/{id}", calendarId)).andExpect(status().isNoContent());

    verify(calendarService).deleteCalendar(calendarId);
  }

  @Test
  @DisplayName("Should return 404 when deleting calendar that doesn't exist")
  void deleteCalendar_WhenCalendarDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID calendarId = UUID.randomUUID();
    doThrow(new CalendarNotFoundException("Calendar not found with id: " + calendarId))
        .when(calendarService)
        .deleteCalendar(calendarId);

    // When/Then
    mockMvc.perform(delete("/api/calendars/{id}", calendarId)).andExpect(status().isNotFound());

    verify(calendarService).deleteCalendar(calendarId);
  }
}