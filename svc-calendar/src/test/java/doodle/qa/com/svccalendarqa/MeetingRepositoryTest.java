package doodle.qa.com.svccalendarqa;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svccalendarqa.entity.Meeting;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import doodle.qa.com.svccalendarqa.repository.MeetingRepository;
import doodle.qa.com.svccalendarqa.repository.UserCalendarRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the MeetingRepository. These tests verify the custom repository methods and JPA
 * functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class MeetingRepositoryTest {

  @Autowired private MeetingRepository meetingRepository;
  @Autowired private UserCalendarRepository userCalendarRepository;

  private UserCalendar userCalendar;
  private UUID userId;
  private UUID calendarId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    calendarId = UUID.randomUUID();
    userCalendar = TestDataFactory.createUserCalendar(null, calendarId, userId);
    userCalendarRepository.save(userCalendar);
  }

  @Test
  @DisplayName("Should find meetings by user calendar and time range")
  void
      findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc_ShouldReturnMeetings() {
    // Given
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime from = now.minusDays(1);
    LocalDateTime to = now.plusDays(1);

    Meeting meeting1 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 1",
            "Description 1",
            now.minusHours(12),
            now.minusHours(11),
            "Location 1",
            userCalendar,
            calendarId);
    Meeting meeting2 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 2",
            "Description 2",
            now,
            now.plusHours(1),
            "Location 2",
            userCalendar,
            calendarId);
    Meeting meeting3 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 3",
            "Description 3",
            now.plusHours(12),
            now.plusHours(13),
            "Location 3",
            userCalendar,
            calendarId);

    meetingRepository.save(meeting1);
    meetingRepository.save(meeting2);
    meetingRepository.save(meeting3);

    // When
    Page<Meeting> meetings =
        meetingRepository
            .findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
                userCalendar, from, to, PageRequest.of(0, 10));

    // Then
    assertThat(meetings.getContent()).hasSize(3);
    assertThat(meetings.getContent().get(0).getTitle()).isEqualTo("Meeting 1");
    assertThat(meetings.getContent().get(1).getTitle()).isEqualTo("Meeting 2");
    assertThat(meetings.getContent().get(2).getTitle()).isEqualTo("Meeting 3");
  }

  @Test
  @DisplayName("Should find meeting by user calendar and ID")
  void findByUserCalendarAndId_WhenMeetingExists_ShouldReturnMeeting() {
    // Given
    Meeting meeting =
        TestDataFactory.createMeeting(
            null,
            "Test Meeting",
            "Test Description",
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            "Test Location",
            userCalendar,
            calendarId);
    Meeting savedMeeting = meetingRepository.save(meeting);

    // When
    Optional<Meeting> foundMeeting =
        meetingRepository.findByUserCalendarAndId(userCalendar, savedMeeting.getId());

    // Then
    assertThat(foundMeeting).isPresent();
    assertThat(foundMeeting.get().getTitle()).isEqualTo("Test Meeting");
    assertThat(foundMeeting.get().getDescription()).isEqualTo("Test Description");
  }

  @Test
  @DisplayName(
      "Should return empty optional when finding meeting by user calendar and ID that doesn't exist")
  void findByUserCalendarAndId_WhenMeetingDoesNotExist_ShouldReturnEmptyOptional() {
    // Given
    UUID nonExistentMeetingId = UUID.randomUUID();

    // When
    Optional<Meeting> foundMeeting =
        meetingRepository.findByUserCalendarAndId(userCalendar, nonExistentMeetingId);

    // Then
    assertThat(foundMeeting).isEmpty();
  }

  @Test
  @DisplayName("Should find overlapping meetings")
  void findOverlappingMeetingsByUserCalendar_ShouldReturnOverlappingMeetings() {
    // Given
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    // Meeting that starts before and ends during the range
    Meeting meeting1 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 1",
            "Description 1",
            now.minusHours(1),
            now.plusHours(1),
            "Location 1",
            userCalendar,
            calendarId);

    // Meeting that starts and ends within the range
    Meeting meeting2 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 2",
            "Description 2",
            now.plusHours(2),
            now.plusHours(3),
            "Location 2",
            userCalendar,
            calendarId);

    // Meeting that starts during and ends after the range
    Meeting meeting3 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 3",
            "Description 3",
            now.plusHours(4),
            now.plusHours(6),
            "Location 3",
            userCalendar,
            calendarId);

    // Meeting that starts before and ends after the range (completely overlaps)
    Meeting meeting4 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 4",
            "Description 4",
            now.minusHours(1),
            now.plusHours(6),
            "Location 4",
            userCalendar,
            calendarId);

    // Meeting that is completely outside the range (before)
    Meeting meeting5 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 5",
            "Description 5",
            now.minusHours(3),
            now.minusHours(2),
            "Location 5",
            userCalendar,
            calendarId);

    // Meeting that is completely outside the range (after)
    Meeting meeting6 =
        TestDataFactory.createMeeting(
            null,
            "Meeting 6",
            "Description 6",
            now.plusHours(7),
            now.plusHours(8),
            "Location 6",
            userCalendar,
            calendarId);

    meetingRepository.save(meeting1);
    meetingRepository.save(meeting2);
    meetingRepository.save(meeting3);
    meetingRepository.save(meeting4);
    meetingRepository.save(meeting5);
    meetingRepository.save(meeting6);

    // When
    LocalDateTime to = now.plusHours(5);
    List<Meeting> overlappingMeetings =
        meetingRepository.findOverlappingMeetingsByUserCalendar(userCalendar, now, to);

    // Then
    assertThat(overlappingMeetings).hasSize(4);
    assertThat(overlappingMeetings)
        .extracting(Meeting::getTitle)
        .containsExactlyInAnyOrder("Meeting 1", "Meeting 2", "Meeting 3", "Meeting 4");
  }

  @Test
  @DisplayName("Should save meeting with all fields")
  void save_WithAllFields_ShouldPersistAllFields() {
    // Given
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    Meeting meeting =
        TestDataFactory.createMeeting(
            null,
            "New Meeting",
            "New Description",
            now,
            now.plusHours(1),
            "New Location",
            userCalendar,
            calendarId);

    // When
    Meeting savedMeeting = meetingRepository.save(meeting);
    Meeting retrievedMeeting = meetingRepository.findById(savedMeeting.getId()).orElseThrow();

    // Then
    assertThat(retrievedMeeting.getTitle()).isEqualTo("New Meeting");
    assertThat(retrievedMeeting.getDescription()).isEqualTo("New Description");
    assertThat(retrievedMeeting.getStartTime()).isEqualTo(now);
    assertThat(retrievedMeeting.getEndTime()).isEqualTo(now.plusHours(1));
    assertThat(retrievedMeeting.getLocation()).isEqualTo("New Location");
    assertThat(retrievedMeeting.getUserCalendar()).isEqualTo(userCalendar);
    assertThat(retrievedMeeting.getCalendarId()).isEqualTo(calendarId);
  }

  @Test
  @DisplayName("Should update meeting when saving with existing ID")
  void save_WithExistingId_ShouldUpdateMeeting() {
    // Given
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    Meeting meeting =
        TestDataFactory.createMeeting(
            null,
            "Original Title",
            "Original Description",
            now,
            now.plusHours(1),
            "Original Location",
            userCalendar,
            calendarId);
    Meeting savedMeeting = meetingRepository.save(meeting);
    UUID meetingId = savedMeeting.getId();

    // When
    savedMeeting.setTitle("Updated Title");
    savedMeeting.setDescription("Updated Description");
    meetingRepository.save(savedMeeting);
    Meeting retrievedMeeting = meetingRepository.findById(meetingId).orElseThrow();

    // Then
    assertThat(retrievedMeeting.getTitle()).isEqualTo("Updated Title");
    assertThat(retrievedMeeting.getDescription()).isEqualTo("Updated Description");
  }

  @Test
  @DisplayName("Should delete meeting when meeting exists")
  void delete_WhenMeetingExists_ShouldRemoveMeeting() {
    // Given
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    Meeting meeting =
        TestDataFactory.createMeeting(
            null,
            "Meeting To Delete",
            "Description",
            now,
            now.plusHours(1),
            "Location",
            userCalendar,
            calendarId);
    Meeting savedMeeting = meetingRepository.save(meeting);
    UUID meetingId = savedMeeting.getId();

    // When
    meetingRepository.delete(savedMeeting);
    Optional<Meeting> retrievedMeeting = meetingRepository.findById(meetingId);

    // Then
    assertThat(retrievedMeeting).isEmpty();
  }
}
