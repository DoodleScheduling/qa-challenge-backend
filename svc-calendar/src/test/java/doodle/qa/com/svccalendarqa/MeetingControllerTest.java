package doodle.qa.com.svccalendarqa;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svccalendarqa.controller.MeetingController;
import doodle.qa.com.svccalendarqa.dto.MeetingDto;
import doodle.qa.com.svccalendarqa.dto.TimeSlotDto;
import doodle.qa.com.svccalendarqa.exception.CalendarNotFoundException;
import doodle.qa.com.svccalendarqa.exception.MeetingNotFoundException;
import doodle.qa.com.svccalendarqa.service.MeetingService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

/**
 * Unit tests for the MeetingController. These tests verify the REST API endpoints using MockMvc.
 */
@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private MeetingService meetingService;

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  @Test
  @DisplayName(
      "Should return meetings when getting meetings for a calendar without explicit pagination")
  void getMeetings_WithDefaultPagination_ShouldReturnMeetings() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);

    List<MeetingDto> meetingDtos = TestDataFactory.createMeetingDtoList(3, calendarId);
    Page<MeetingDto> meetingPage =
        new PageImpl<>(meetingDtos, PageRequest.of(0, 10), meetingDtos.size());

    when(meetingService.findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)))
        .thenReturn(meetingPage);

    // When/Then
    mockMvc
        .perform(
            get("/meeting")
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString())
                .param("from", from.format(DATE_TIME_FORMATTER))
                .param("to", to.format(DATE_TIME_FORMATTER)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.meetings", hasSize(3)))
        .andExpect(jsonPath("$.meetings[0].title", is(meetingDtos.get(0).getTitle())))
        .andExpect(jsonPath("$.meetings[1].title", is(meetingDtos.get(1).getTitle())))
        .andExpect(jsonPath("$.meetings[2].title", is(meetingDtos.get(2).getTitle())))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(0)));

    verify(meetingService)
        .findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should return paginated meetings when getting meetings with pagination parameters")
  void getMeetings_WithPagination_ShouldReturnPagedMeetings() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);
    int pageSize = 2;
    int pageNumber = 1; // Second page (0-indexed)

    List<MeetingDto> allMeetings = TestDataFactory.createMeetingDtoList(5, calendarId);

    // Create a Page object with the meetings for the second page
    Page<MeetingDto> meetingPage =
        new PageImpl<>(
            allMeetings.subList(
                pageNumber * pageSize, Math.min((pageNumber + 1) * pageSize, allMeetings.size())),
            PageRequest.of(pageNumber, pageSize),
            allMeetings.size());

    when(meetingService.findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)))
        .thenReturn(meetingPage);

    // When/Then
    mockMvc
        .perform(
            get("/meeting")
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString())
                .param("from", from.format(DATE_TIME_FORMATTER))
                .param("to", to.format(DATE_TIME_FORMATTER))
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.meetings", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(
            jsonPath("$.totalPages", is((int) Math.ceil((double) allMeetings.size() / pageSize))));

    verify(meetingService)
        .findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page when getting meetings with page number out of bounds")
  void getMeetings_WithPageOutOfBounds_ShouldReturnEmptyPage() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);
    int pageSize = 10;
    int pageNumber = 10; // Page that doesn't exist

    List<MeetingDto> allMeetings = TestDataFactory.createMeetingDtoList(5, calendarId);

    // Create an empty Page object
    Page<MeetingDto> emptyPage =
        new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), allMeetings.size());

    when(meetingService.findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)))
        .thenReturn(emptyPage);

    // When/Then
    mockMvc
        .perform(
            get("/meeting")
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString())
                .param("from", from.format(DATE_TIME_FORMATTER))
                .param("to", to.format(DATE_TIME_FORMATTER))
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.meetings", hasSize(0)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is(1)));

    verify(meetingService)
        .findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should return 404 when getting meetings for a calendar that doesn't exist")
  void getMeetings_WhenCalendarNotFound_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);

    when(meetingService.findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)))
        .thenThrow(new CalendarNotFoundException(calendarId, userId));

    // When/Then
    mockMvc
        .perform(
            get("/meeting")
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString())
                .param("from", from.format(DATE_TIME_FORMATTER))
                .param("to", to.format(DATE_TIME_FORMATTER)))
        .andExpect(status().isNotFound());

    verify(meetingService)
        .findMeetings(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class));
  }

  @Test
  @DisplayName(
      "Should return available time slots when getting available time slots for a calendar")
  void getAvailableTimeSlots_ShouldReturnTimeSlots() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
    LocalDateTime to = from.plusHours(4);
    int slotDurationMinutes = 60;

    List<TimeSlotDto> timeSlots = TestDataFactory.createTimeSlotDtoList(4, slotDurationMinutes);
    Page<TimeSlotDto> timeSlotsPage =
        new PageImpl<>(timeSlots, PageRequest.of(0, 10), timeSlots.size());

    when(meetingService.findAvailableTimeSlots(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(slotDurationMinutes),
            any(Pageable.class)))
        .thenReturn(timeSlotsPage);

    // When/Then
    mockMvc
        .perform(
            get("/meeting/slots")
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString())
                .param("from", from.format(DATE_TIME_FORMATTER))
                .param("to", to.format(DATE_TIME_FORMATTER))
                .param("slotDuration", String.valueOf(slotDurationMinutes)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.slots", hasSize(4)))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(0)));

    verify(meetingService)
        .findAvailableTimeSlots(
            eq(userId),
            eq(calendarId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(slotDurationMinutes),
            any(Pageable.class));
  }

  @Test
  @DisplayName("Should return meeting when getting meeting by ID that exists")
  void getMeeting_WhenMeetingExists_ShouldReturnMeeting() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingDto =
        TestDataFactory.createMeetingDto(
            meetingId,
            "Test Meeting",
            "Test Description",
            startTime,
            endTime,
            "Test Location",
            calendarId);

    when(meetingService.findMeeting(meetingId, userId, calendarId)).thenReturn(meetingDto);

    // When/Then
    mockMvc
        .perform(
            get("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(meetingId.toString())))
        .andExpect(jsonPath("$.title", is("Test Meeting")))
        .andExpect(jsonPath("$.description", is("Test Description")));

    verify(meetingService).findMeeting(meetingId, userId, calendarId);
  }

  @Test
  @DisplayName("Should return 404 when getting meeting by ID that doesn't exist")
  void getMeeting_WhenMeetingDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    when(meetingService.findMeeting(meetingId, userId, calendarId))
        .thenThrow(new MeetingNotFoundException(meetingId, userId, calendarId));

    // When/Then
    mockMvc
        .perform(
            get("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString()))
        .andExpect(status().isNotFound());

    verify(meetingService).findMeeting(meetingId, userId, calendarId);
  }

  @Test
  @DisplayName("Should create meeting when creating meeting with valid data")
  void createMeeting_WithValidData_ShouldCreateMeeting() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();

    LocalDateTime startTime = LocalDateTime.now().plusHours(1);
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingToCreate =
        TestDataFactory.createMeetingDto(
            null, "New Meeting", "New Description", startTime, endTime, "New Location", calendarId);

    MeetingDto createdMeeting =
        TestDataFactory.createMeetingDto(
            UUID.randomUUID(),
            "New Meeting",
            "New Description",
            startTime,
            endTime,
            "New Location",
            calendarId);

    when(meetingService.createMeeting(any(MeetingDto.class), eq(userId)))
        .thenReturn(createdMeeting);

    // When/Then
    mockMvc
        .perform(
            post("/meeting")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(meetingToCreate)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(createdMeeting.getId().toString())))
        .andExpect(jsonPath("$.title", is("New Meeting")))
        .andExpect(jsonPath("$.description", is("New Description")));

    verify(meetingService).createMeeting(any(MeetingDto.class), eq(userId));
  }

  @Test
  @DisplayName("Should return 400 when creating meeting with invalid data")
  void createMeeting_WithInvalidData_ShouldReturn400() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();

    // Missing required fields
    MeetingDto invalidMeeting = MeetingDto.builder().calendarId(calendarId).build();

    // When/Then
    mockMvc
        .perform(
            post("/meeting")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMeeting)))
        .andExpect(status().isBadRequest());

    verify(meetingService, never()).createMeeting(any(MeetingDto.class), any(UUID.class));
  }

  @Test
  @DisplayName("Should update meeting when updating meeting that exists with valid data")
  void updateMeeting_WhenMeetingExistsWithValidData_ShouldUpdateMeeting() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    LocalDateTime startTime = LocalDateTime.now().plusHours(1);
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingToUpdate =
        TestDataFactory.createMeetingDto(
            meetingId,
            "Updated Meeting",
            "Updated Description",
            startTime,
            endTime,
            "Updated Location",
            calendarId);

    when(meetingService.updateMeeting(eq(meetingId), any(MeetingDto.class), eq(userId)))
        .thenReturn(meetingToUpdate);

    // When/Then
    mockMvc
        .perform(
            put("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(meetingToUpdate)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(meetingId.toString())))
        .andExpect(jsonPath("$.title", is("Updated Meeting")))
        .andExpect(jsonPath("$.description", is("Updated Description")));

    verify(meetingService).updateMeeting(eq(meetingId), any(MeetingDto.class), eq(userId));
  }

  @Test
  @DisplayName("Should return 404 when updating meeting that doesn't exist")
  void updateMeeting_WhenMeetingDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    LocalDateTime startTime = LocalDateTime.now().plusHours(1);
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingToUpdate =
        TestDataFactory.createMeetingDto(
            meetingId,
            "Updated Meeting",
            "Updated Description",
            startTime,
            endTime,
            "Updated Location",
            calendarId);

    when(meetingService.updateMeeting(eq(meetingId), any(MeetingDto.class), eq(userId)))
        .thenThrow(new MeetingNotFoundException(meetingId, userId, calendarId));

    // When/Then
    mockMvc
        .perform(
            put("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(meetingToUpdate)))
        .andExpect(status().isNotFound());

    verify(meetingService).updateMeeting(eq(meetingId), any(MeetingDto.class), eq(userId));
  }

  @Test
  @DisplayName("Should delete meeting when deleting meeting that exists")
  void deleteMeeting_WhenMeetingExists_ShouldDeleteMeeting() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    doNothing().when(meetingService).deleteMeeting(meetingId, userId, calendarId);

    // When/Then
    mockMvc
        .perform(
            delete("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString()))
        .andExpect(status().isNoContent());

    verify(meetingService).deleteMeeting(meetingId, userId, calendarId);
  }

  @Test
  @DisplayName("Should return 404 when deleting meeting that doesn't exist")
  void deleteMeeting_WhenMeetingDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();

    doThrow(new MeetingNotFoundException(meetingId, userId, calendarId))
        .when(meetingService)
        .deleteMeeting(meetingId, userId, calendarId);

    // When/Then
    mockMvc
        .perform(
            delete("/meeting/{id}", meetingId)
                .param("userId", userId.toString())
                .param("calendarId", calendarId.toString()))
        .andExpect(status().isNotFound());

    verify(meetingService).deleteMeeting(meetingId, userId, calendarId);
  }
}
