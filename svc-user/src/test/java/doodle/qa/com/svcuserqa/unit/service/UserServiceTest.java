package doodle.qa.com.svcuserqa.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.svcuser.avro.EventType;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.exception.CalendarAlreadyExistsException;
import doodle.qa.com.svcuserqa.exception.CalendarLimitExceededException;
import doodle.qa.com.svcuserqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcuserqa.exception.UserNotFoundException;
import doodle.qa.com.svcuserqa.kafka.UserStateProducer;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.ArrayList;
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

/**
 * Unit tests for the UserService. These tests verify the business logic in the service layer using
 * mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private UserStateProducer userStateProducer;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, userStateProducer);
  }

  @Test
  @DisplayName("Should return all users when getting all users")
  void getAllUsers_ShouldReturnAllUsers() {
    // Given
    List<User> users = TestDataFactory.createUserList(3);
    when(userRepository.findAll()).thenReturn(users);

    // When
    List<UserDto> result = userService.getAllUsers();

    // Then
    assertThat(result).hasSize(3);
    verify(userRepository).findAll();
  }

  @Test
  @DisplayName("Should return user by ID when user exists")
  void getUserById_WhenUserExists_ShouldReturnUser() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.createUser(userId, "Test User", "test@example.com", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // When
    UserDto result = userService.getUserById(userId);

    // Then
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getName()).isEqualTo("Test User");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when getting user by ID that doesn't exist")
  void getUserById_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("Should create user and send Kafka message when creating user")
  void createUser_ShouldCreateUserAndSendKafkaMessage() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("New User", "new@example.com");
    User savedUser =
        TestDataFactory.createUser(UUID.randomUUID(), "New User", "new@example.com", null);
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // When
    UserDto result = userService.createUser(userDto);

    // Then
    assertThat(result.getId()).isEqualTo(savedUser.getId());
    assertThat(result.getName()).isEqualTo("New User");
    assertThat(result.getEmail()).isEqualTo("new@example.com");

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.getName()).isEqualTo("New User");
    assertThat(capturedUser.getEmail()).isEqualTo("new@example.com");

    verify(userStateProducer).sendUserState(savedUser, EventType.CREATED);
  }

  @Test
  @DisplayName("Should update user and send Kafka message when updating user that exists")
  void updateUser_WhenUserExists_ShouldUpdateUserAndSendKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();
    UserDto userDto =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", null);
    User existingUser =
        TestDataFactory.createUser(userId, "Original User", "original@example.com", null);
    User updatedUser =
        TestDataFactory.createUser(userId, "Updated User", "updated@example.com", null);

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);

    // When
    UserDto result = userService.updateUser(userId, userDto);

    // Then
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getName()).isEqualTo("Updated User");
    assertThat(result.getEmail()).isEqualTo("updated@example.com");

    verify(userRepository).findById(userId);
    verify(userRepository).save(existingUser);
    verify(userStateProducer).sendUserState(updatedUser, EventType.UPDATED);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when updating user that doesn't exist")
  void updateUser_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UserDto userDto =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", null);
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, userDto));
    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(userStateProducer, never()).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName("Should delete user and send Kafka message when deleting user that exists")
  void deleteUser_WhenUserExists_ShouldDeleteUserAndSendKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();
    User existingUser =
        TestDataFactory.createUser(userId, "User To Delete", "delete@example.com", null);
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    // When
    userService.deleteUser(userId);

    // Then
    verify(userRepository).findById(userId);
    verify(userRepository).delete(existingUser);
    verify(userStateProducer).sendUserState(existingUser, EventType.DELETED);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting user that doesn't exist")
  void deleteUser_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
    verify(userRepository).findById(userId);
    verify(userRepository, never()).delete(any(User.class));
    verify(userStateProducer, never()).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName(
      "Should add calendar to user and send Kafka message when user exists and calendar not already added")
  void addCalendarToUser_WhenUserExistsAndCalendarNotAdded_ShouldAddCalendarAndSendKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    User existingUser = TestDataFactory.createUser(userId, "User", "user@example.com", null);
    User updatedUser =
        TestDataFactory.createUser(userId, "User", "user@example.com", List.of(calendarId));

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);

    // When
    UserDto result = userService.addCalendarToUser(userId, calendarId);

    // Then
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getCalendarIds()).contains(calendarId);

    verify(userRepository).findById(userId);
    verify(userRepository).save(existingUser);
    verify(userStateProducer).sendUserState(updatedUser, EventType.CALENDAR_ADDED);
  }

  @Test
  @DisplayName("Should throw exception when user exists but calendar already added")
  void
      addCalendarToUser_WhenUserExistsAndCalendarAlreadyAdded_ShouldThrowCalendarAlreadyExistsException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    User existingUser =
        TestDataFactory.createUser(userId, "User", "user@example.com", List.of(calendarId));

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    // When/Then
    assertThrows(
        CalendarAlreadyExistsException.class,
        () -> userService.addCalendarToUser(userId, calendarId));

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(userStateProducer, never()).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName("Should throw exception when user has reached the maximum limit of 10 calendars")
  void addCalendarToUser_WhenUserHasMaxCalendars_ShouldThrowCalendarLimitExceededException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID newCalendarId = UUID.randomUUID();

    // Create a list of 10 calendar IDs
    List<UUID> existingCalendarIds = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      existingCalendarIds.add(UUID.randomUUID());
    }

    User existingUser =
        TestDataFactory.createUser(userId, "User", "user@example.com", existingCalendarIds);

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    // When/Then
    assertThrows(
        CalendarLimitExceededException.class,
        () -> userService.addCalendarToUser(userId, newCalendarId));

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(userStateProducer, never()).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName(
      "Should remove calendar from user and send Kafka message when user exists and calendar is present")
  void
      removeCalendarFromUser_WhenUserExistsAndCalendarPresent_ShouldRemoveCalendarAndSendKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    List<UUID> calendarIds = new ArrayList<>();
    calendarIds.add(calendarId);
    User existingUser = TestDataFactory.createUser(userId, "User", "user@example.com", calendarIds);
    User updatedUser =
        TestDataFactory.createUser(userId, "User", "user@example.com", new ArrayList<>());

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);

    // When
    UserDto result = userService.removeCalendarFromUser(userId, calendarId);

    // Then
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getCalendarIds()).doesNotContain(calendarId);

    verify(userRepository).findById(userId);
    verify(userRepository).save(existingUser);
    verify(userStateProducer).sendUserState(updatedUser, EventType.CALENDAR_REMOVED);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when user exists but calendar not present")
  void
      removeCalendarFromUser_WhenUserExistsAndCalendarNotPresent_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UUID otherCalendarId = UUID.randomUUID();
    User existingUser =
        TestDataFactory.createUser(userId, "User", "user@example.com", List.of(otherCalendarId));

    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    // When/Then
    assertThrows(
        CalendarNotFoundException.class,
        () -> userService.removeCalendarFromUser(userId, calendarId));

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(userStateProducer, never()).sendUserState(any(User.class), any(EventType.class));
  }
}
