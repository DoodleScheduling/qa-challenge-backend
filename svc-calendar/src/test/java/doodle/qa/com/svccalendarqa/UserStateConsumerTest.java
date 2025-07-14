package doodle.qa.com.svccalendarqa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import doodle.qa.com.svccalendarqa.kafka.UserStateConsumer;
import doodle.qa.com.svccalendarqa.repository.UserCalendarRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Unit tests for the UserStateConsumer. These tests verify the Kafka message consumption
 * functionality.
 */
@ExtendWith(MockitoExtension.class)
class UserStateConsumerTest {

  @Mock private UserCalendarRepository userCalendarRepository;
  @Mock private Acknowledgment acknowledgment;

  private UserStateConsumer userStateConsumer;

  @BeforeEach
  void setUp() {
    userStateConsumer = new UserStateConsumer(userCalendarRepository);
  }

  @Test
  @DisplayName("Should process user created event and save user calendars")
  void processUserState_WhenUserCreated_ShouldSaveUserCalendars() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId1 = UUID.randomUUID();
    UUID calendarId2 = UUID.randomUUID();
    List<String> calendarIds = Arrays.asList(calendarId1.toString(), calendarId2.toString());

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Test User")
            .setEmail("test@example.com")
            .setCalendarIds(calendarIds)
            .setEventType(EventType.CREATED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Mock repository behavior
    when(userCalendarRepository.existsByCalendarIdAndUserId(calendarId1, userId)).thenReturn(false);
    when(userCalendarRepository.existsByCalendarIdAndUserId(calendarId2, userId)).thenReturn(false);
    when(userCalendarRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

    // When
    userStateConsumer.processUserState(userState, acknowledgment);

    // Then
    // Verify that the repository was called to save the user calendars
    ArgumentCaptor<UserCalendar> userCalendarCaptor = ArgumentCaptor.forClass(UserCalendar.class);
    verify(userCalendarRepository, times(2)).save(userCalendarCaptor.capture());

    List<UserCalendar> capturedUserCalendars = userCalendarCaptor.getAllValues();
    verify(userCalendarRepository).findByUserId(userId);
    verify(acknowledgment).acknowledge();

    // Verify the saved user calendars
    assertUserCalendars(capturedUserCalendars, userId, Arrays.asList(calendarId1, calendarId2));
  }

  @Test
  @DisplayName("Should process user updated event and update user calendars")
  void processUserState_WhenUserUpdated_ShouldUpdateUserCalendars() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId1 = UUID.randomUUID();
    UUID calendarId2 = UUID.randomUUID();
    UUID oldCalendarId = UUID.randomUUID();
    List<String> calendarIds = Arrays.asList(calendarId1.toString(), calendarId2.toString());

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Updated User")
            .setEmail("updated@example.com")
            .setCalendarIds(calendarIds)
            .setEventType(EventType.UPDATED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Create existing user calendar that should be removed
    UserCalendar existingUserCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), oldCalendarId, userId);

    // Mock repository behavior
    when(userCalendarRepository.existsByCalendarIdAndUserId(calendarId1, userId)).thenReturn(false);
    when(userCalendarRepository.existsByCalendarIdAndUserId(calendarId2, userId)).thenReturn(true);
    when(userCalendarRepository.findByUserId(userId))
        .thenReturn(Collections.singletonList(existingUserCalendar));

    // When
    userStateConsumer.processUserState(userState, acknowledgment);

    // Then
    // Verify that the repository was called to save the new user calendar
    ArgumentCaptor<UserCalendar> userCalendarCaptor = ArgumentCaptor.forClass(UserCalendar.class);
    verify(userCalendarRepository).save(userCalendarCaptor.capture());

    UserCalendar capturedUserCalendar = userCalendarCaptor.getValue();
    verify(userCalendarRepository).findByUserId(userId);
    verify(userCalendarRepository).delete(existingUserCalendar);
    verify(acknowledgment).acknowledge();

    // Verify the saved user calendar
    assertUserCalendar(capturedUserCalendar, userId, calendarId1);
  }

  @Test
  @DisplayName("Should process user deleted event and delete user calendars")
  void processUserState_WhenUserDeleted_ShouldDeleteUserCalendars() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId1 = UUID.randomUUID();
    UUID calendarId2 = UUID.randomUUID();

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Deleted User")
            .setEmail("deleted@example.com")
            .setCalendarIds(Collections.emptyList())
            .setEventType(EventType.DELETED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Create existing user calendars that should be deleted
    UserCalendar userCalendar1 =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId1, userId);
    UserCalendar userCalendar2 =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), calendarId2, userId);
    List<UserCalendar> existingUserCalendars = Arrays.asList(userCalendar1, userCalendar2);

    // Mock repository behavior
    when(userCalendarRepository.findByUserId(userId)).thenReturn(existingUserCalendars);

    // When
    userStateConsumer.processUserState(userState, acknowledgment);

    // Then
    // Verify that the repository was called to delete the user calendars
    verify(userCalendarRepository).findByUserId(userId);
    verify(userCalendarRepository).deleteAll(existingUserCalendars);
    verify(acknowledgment).acknowledge();
  }

  @Test
  @DisplayName("Should process calendar added event and add user calendars")
  void processUserState_WhenCalendarAdded_ShouldAddUserCalendars() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    List<String> calendarIds = Collections.singletonList(calendarId.toString());

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Test User")
            .setEmail("test@example.com")
            .setCalendarIds(calendarIds)
            .setEventType(EventType.CALENDAR_ADDED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Mock repository behavior
    when(userCalendarRepository.existsByCalendarIdAndUserId(calendarId, userId)).thenReturn(false);

    // When
    userStateConsumer.processUserState(userState, acknowledgment);

    // Then
    // Verify that the repository was called to save the user calendar
    ArgumentCaptor<UserCalendar> userCalendarCaptor = ArgumentCaptor.forClass(UserCalendar.class);
    verify(userCalendarRepository).save(userCalendarCaptor.capture());

    UserCalendar capturedUserCalendar = userCalendarCaptor.getValue();
    verify(acknowledgment).acknowledge();

    // Verify the saved user calendar
    assertUserCalendar(capturedUserCalendar, userId, calendarId);
  }

  @Test
  @DisplayName("Should process calendar removed event and remove user calendars")
  void processUserState_WhenCalendarRemoved_ShouldRemoveUserCalendars() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID remainingCalendarId = UUID.randomUUID();
    UUID removedCalendarId = UUID.randomUUID();
    List<String> remainingCalendarIds = Collections.singletonList(remainingCalendarId.toString());

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Test User")
            .setEmail("test@example.com")
            .setCalendarIds(remainingCalendarIds)
            .setEventType(EventType.CALENDAR_REMOVED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Create existing user calendars
    UserCalendar remainingUserCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), remainingCalendarId, userId);
    UserCalendar removedUserCalendar =
        TestDataFactory.createUserCalendar(UUID.randomUUID(), removedCalendarId, userId);
    List<UserCalendar> existingUserCalendars =
        Arrays.asList(remainingUserCalendar, removedUserCalendar);

    // Mock repository behavior
    when(userCalendarRepository.findByUserId(userId)).thenReturn(existingUserCalendars);

    // When
    userStateConsumer.processUserState(userState, acknowledgment);

    // Then
    // Verify that the repository was called to delete the removed user calendar
    verify(userCalendarRepository).findByUserId(userId);
    verify(userCalendarRepository).delete(removedUserCalendar);
    verify(userCalendarRepository, never()).delete(remainingUserCalendar);
    verify(acknowledgment).acknowledge();
  }

  @Test
  @DisplayName("Should handle exception during processing")
  void processUserState_WhenExceptionOccurs_ShouldNotAcknowledge() {
    // Given
    UUID userId = UUID.randomUUID();
    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Test User")
            .setEmail("test@example.com")
            .setCalendarIds(Collections.emptyList())
            .setEventType(EventType.CREATED)
            .setTimestamp(System.currentTimeMillis())
            .build();

    // Mock repository to throw exception
    when(userCalendarRepository.findByUserId(userId))
        .thenThrow(new RuntimeException("Test exception"));

    // When/Then
    try {
      userStateConsumer.processUserState(userState, acknowledgment);
    } catch (Exception e) {
      // Expected exception
    }

    // Verify that the acknowledgment was not called
    verify(acknowledgment, never()).acknowledge();
  }

  // Helper methods to verify user calendars
  private void assertUserCalendar(UserCalendar userCalendar, UUID userId, UUID calendarId) {
    assertThat(userCalendar.getUserId()).isEqualTo(userId);
    assertThat(userCalendar.getCalendarId()).isEqualTo(calendarId);
  }

  private void assertUserCalendars(
      List<UserCalendar> userCalendars, UUID userId, List<UUID> calendarIds) {
    assertThat(userCalendars).hasSize(calendarIds.size());
    for (int i = 0; i < userCalendars.size(); i++) {
      assertUserCalendar(userCalendars.get(i), userId, calendarIds.get(i));
    }
  }
}
