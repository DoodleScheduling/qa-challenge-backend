package doodle.qa.com.svccalendarqa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import doodle.qa.com.svccalendarqa.dto.MeetingDto;
import doodle.qa.com.svccalendarqa.dto.TimeSlotDto;
import doodle.qa.com.svccalendarqa.entity.Meeting;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import doodle.qa.com.svccalendarqa.exception.CalendarNotFoundException;
import doodle.qa.com.svccalendarqa.exception.IllegalArgumentException;
import doodle.qa.com.svccalendarqa.exception.MeetingNotFoundException;
import doodle.qa.com.svccalendarqa.repository.MeetingRepository;
import doodle.qa.com.svccalendarqa.repository.UserCalendarRepository;
import doodle.qa.com.svccalendarqa.service.MeetingService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for the MeetingService. These tests verify the business logic in the service layer
 * using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private UserCalendarRepository userCalendarRepository;
  @Mock private RestTemplate restTemplate;

  private MeetingService meetingService;

  @BeforeEach
  void setUp() {
    meetingService = new MeetingService(meetingRepository, userCalendarRepository, restTemplate);
  }

  @Test
  @DisplayName("Should return all meetings when getting meetings for a calendar")
  void findMeetings_ShouldReturnAllMeetings() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    List<Meeting> meetings = TestDataFactory.createMeetingList(3, userCalendar, calendarId);
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Meeting> meetingsPage = new PageImpl<>(meetings, pageable, meetings.size());

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository
            .findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
                userCalendar, from, to, pageable))
        .thenReturn(meetingsPage);

    // When
    Page<MeetingDto> result = meetingService.findMeetings(userId, calendarId, from, to, pageable);

    // Then
    assertThat(result.getContent()).hasSize(3);
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository)
        .findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            userCalendar, from, to, pageable);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when calendar not found")
  void findMeetings_WhenCalendarNotFound_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime from = LocalDateTime.now().minusDays(1);
    LocalDateTime to = LocalDateTime.now().plusDays(1);
    Pageable pageable = PageRequest.of(0, 10);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        CalendarNotFoundException.class,
        () -> meetingService.findMeetings(userId, calendarId, from, to, pageable));
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository, never())
        .findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            any(), any(), any(), any());
  }

  @Test
  @DisplayName("Should return meeting by ID when meeting exists")
  void findMeeting_WhenMeetingExists_ShouldReturnMeeting() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    Meeting meeting =
        TestDataFactory.createMeeting(
            meetingId,
            "Test Meeting",
            "Test Description",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            "Test Location",
            userCalendar,
            calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.of(meeting));

    // When
    MeetingDto result = meetingService.findMeeting(meetingId, userId, calendarId);

    // Then
    assertThat(result.getId()).isEqualTo(meetingId);
    assertThat(result.getTitle()).isEqualTo("Test Meeting");
    assertThat(result.getDescription()).isEqualTo("Test Description");
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository).findByUserCalendarAndId(userCalendar, meetingId);
  }

  @Test
  @DisplayName("Should throw MeetingNotFoundException when meeting not found")
  void findMeeting_WhenMeetingNotFound_ShouldThrowMeetingNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        MeetingNotFoundException.class,
        () -> meetingService.findMeeting(meetingId, userId, calendarId));
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository).findByUserCalendarAndId(userCalendar, meetingId);
  }

  @Test
  @DisplayName("Should create meeting when valid data provided")
  void createMeeting_WhenValidData_ShouldCreateMeeting() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    LocalDateTime startTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingDto =
        TestDataFactory.createMeetingDto(
            null, "New Meeting", "New Description", startTime, endTime, "New Location", calendarId);

    Meeting savedMeeting =
        TestDataFactory.createMeeting(
            UUID.randomUUID(),
            "New Meeting",
            "New Description",
            startTime,
            endTime,
            "New Location",
            userCalendar,
            calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(userCalendarRepository.findAllByCalendarId(calendarId))
        .thenReturn(Collections.singletonList(userCalendar));
    when(meetingRepository.findOverlappingMeetingsByUserCalendar(
            eq(userCalendar), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    @SuppressWarnings("rawtypes")
    ResponseEntity<List> mockResponse =
        new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
    when(restTemplate.getForEntity(anyString(), eq(List.class))).thenReturn(mockResponse);

    when(meetingRepository.save(any(Meeting.class))).thenReturn(savedMeeting);

    // When
    MeetingDto result = meetingService.createMeeting(meetingDto, userId);

    // Then
    assertThat(result.getId()).isEqualTo(savedMeeting.getId());
    assertThat(result.getTitle()).isEqualTo("New Meeting");
    assertThat(result.getDescription()).isEqualTo("New Description");
    assertThat(result.getStartTime()).isEqualTo(startTime);
    assertThat(result.getEndTime()).isEqualTo(endTime);
    assertThat(result.getLocation()).isEqualTo("New Location");
    assertThat(result.getCalendarId()).isEqualTo(calendarId);

    ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
    verify(meetingRepository).save(meetingCaptor.capture());
    Meeting capturedMeeting = meetingCaptor.getValue();
    assertThat(capturedMeeting.getTitle()).isEqualTo("New Meeting");
    assertThat(capturedMeeting.getDescription()).isEqualTo("New Description");
    assertThat(capturedMeeting.getStartTime()).isEqualTo(startTime);
    assertThat(capturedMeeting.getEndTime()).isEqualTo(endTime);
    assertThat(capturedMeeting.getLocation()).isEqualTo("New Location");
    assertThat(capturedMeeting.getUserCalendar()).isEqualTo(userCalendar);
    assertThat(capturedMeeting.getCalendarId()).isEqualTo(calendarId);
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when end time is before start time")
  void createMeeting_WhenEndTimeBeforeStartTime_ShouldThrowIllegalArgumentException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    LocalDateTime startTime = LocalDateTime.now().plusHours(2);
    LocalDateTime endTime = LocalDateTime.now().plusHours(1); // End time before start time

    MeetingDto meetingDto =
        TestDataFactory.createMeetingDto(
            null, "New Meeting", "New Description", startTime, endTime, "New Location", calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));

    // When/Then
    assertThrows(
        IllegalArgumentException.class, () -> meetingService.createMeeting(meetingDto, userId));
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository, never()).save(any(Meeting.class));
  }

  @Test
  @DisplayName("Should update meeting when valid data provided")
  void updateMeeting_WhenValidData_ShouldUpdateMeeting() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    LocalDateTime startTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime endTime = startTime.plusHours(1);

    MeetingDto meetingDto =
        TestDataFactory.createMeetingDto(
            meetingId,
            "Updated Meeting",
            "Updated Description",
            startTime,
            endTime,
            "Updated Location",
            calendarId);

    Meeting existingMeeting =
        TestDataFactory.createMeeting(
            meetingId,
            "Original Meeting",
            "Original Description",
            startTime.minusDays(1),
            endTime.minusDays(1),
            "Original Location",
            userCalendar,
            calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.of(existingMeeting));
    when(userCalendarRepository.findAllByCalendarId(calendarId))
        .thenReturn(Collections.singletonList(userCalendar));
    when(meetingRepository.findOverlappingMeetingsByUserCalendar(
            eq(userCalendar), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    @SuppressWarnings("rawtypes")
    ResponseEntity<List> mockResponse =
        new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
    when(restTemplate.getForEntity(anyString(), eq(List.class))).thenReturn(mockResponse);

    when(meetingRepository.save(any(Meeting.class))).thenAnswer(i -> i.getArgument(0));

    // When
    MeetingDto result = meetingService.updateMeeting(meetingId, meetingDto, userId);

    // Then
    assertThat(result.getId()).isEqualTo(meetingId);
    assertThat(result.getTitle()).isEqualTo("Updated Meeting");
    assertThat(result.getDescription()).isEqualTo("Updated Description");
    assertThat(result.getStartTime()).isEqualTo(startTime);
    assertThat(result.getEndTime()).isEqualTo(endTime);
    assertThat(result.getLocation()).isEqualTo("Updated Location");
    assertThat(result.getCalendarId()).isEqualTo(calendarId);

    ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
    verify(meetingRepository).save(meetingCaptor.capture());
    Meeting capturedMeeting = meetingCaptor.getValue();
    assertThat(capturedMeeting.getTitle()).isEqualTo("Updated Meeting");
    assertThat(capturedMeeting.getDescription()).isEqualTo("Updated Description");
    assertThat(capturedMeeting.getStartTime()).isEqualTo(startTime);
    assertThat(capturedMeeting.getEndTime()).isEqualTo(endTime);
    assertThat(capturedMeeting.getLocation()).isEqualTo("Updated Location");
  }

  @Test
  @DisplayName("Should throw MeetingNotFoundException when updating meeting that doesn't exist")
  void updateMeeting_WhenMeetingNotFound_ShouldThrowMeetingNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    MeetingDto meetingDto =
        TestDataFactory.createMeetingDto(
            meetingId,
            "Updated Meeting",
            "Updated Description",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            "Updated Location",
            calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        MeetingNotFoundException.class,
        () -> meetingService.updateMeeting(meetingId, meetingDto, userId));
    verify(userCalendarRepository).findByCalendarIdAndUserId(calendarId, userId);
    verify(meetingRepository).findByUserCalendarAndId(userCalendar, meetingId);
    verify(meetingRepository, never()).save(any(Meeting.class));
  }

  @Test
  @DisplayName("Should delete meeting when meeting exists")
  void deleteMeeting_WhenMeetingExists_ShouldDeleteMeeting() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    Meeting meeting =
        TestDataFactory.createMeeting(
            meetingId,
            "Test Meeting",
            "Test Description",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            "Test Location",
            userCalendar,
            calendarId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.of(meeting));
    doNothing().when(meetingRepository).delete(meeting);

    // When
    meetingService.deleteMeeting(meetingId, userId, calendarId);

    // Then
    verify(meetingRepository).delete(meeting);
  }

  @Test
  @DisplayName("Should throw MeetingNotFoundException when deleting meeting that doesn't exist")
  void deleteMeeting_WhenMeetingNotFound_ShouldThrowMeetingNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findByUserCalendarAndId(userCalendar, meetingId))
        .thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        MeetingNotFoundException.class,
        () -> meetingService.deleteMeeting(meetingId, userId, calendarId));
    verify(meetingRepository, never()).delete(any(Meeting.class));
  }

  @Test
  @DisplayName("Should find available time slots when no conflicts exist")
  void findAvailableTimeSlots_WhenNoConflicts_ShouldReturnAvailableSlots() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserCalendar userCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId, userId);
    LocalDateTime from = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime to = from.plusHours(2);
    int slotDurationMinutes = 30;
    Pageable pageable = PageRequest.of(0, 10);

    when(userCalendarRepository.findByCalendarIdAndUserId(calendarId, userId))
        .thenReturn(Optional.of(userCalendar));
    when(meetingRepository.findOverlappingMeetingsByUserCalendar(userCalendar, from, to))
        .thenReturn(Collections.emptyList());

    @SuppressWarnings("rawtypes")
    ResponseEntity<List> mockResponse =
        new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
    when(restTemplate.getForEntity(anyString(), eq(List.class))).thenReturn(mockResponse);

    // When
    Page<TimeSlotDto> result =
        meetingService.findAvailableTimeSlots(
            userId, calendarId, from, to, slotDurationMinutes, pageable);

    // Then
    assertThat(result.getContent()).hasSize(4);
    assertThat(result.getContent().get(0).getStartTime()).isEqualTo(from);
    assertThat(result.getContent().get(0).getEndTime()).isEqualTo(from.plusMinutes(30));
    assertThat(result.getContent().get(1).getStartTime()).isEqualTo(from.plusMinutes(30));
    assertThat(result.getContent().get(1).getEndTime()).isEqualTo(from.plusMinutes(60));
    assertThat(result.getContent().get(2).getStartTime()).isEqualTo(from.plusMinutes(60));
    assertThat(result.getContent().get(2).getEndTime()).isEqualTo(from.plusMinutes(90));
    assertThat(result.getContent().get(3).getStartTime()).isEqualTo(from.plusMinutes(90));
    assertThat(result.getContent().get(3).getEndTime()).isEqualTo(from.plusMinutes(120));
  }
}
