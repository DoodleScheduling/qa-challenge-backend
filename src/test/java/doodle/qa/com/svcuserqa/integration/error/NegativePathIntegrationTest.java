package doodle.qa.com.svcuserqa.integration.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.exception.CalendarAlreadyExistsException;
import doodle.qa.com.svcuserqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcuserqa.exception.UserNotFoundException;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests focusing on negative paths and error scenarios. These tests verify that the
 * application correctly handles error conditions and edge cases in an integrated environment.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-state-test", "user-state-test.DLT"})
class NegativePathIntegrationTest {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when getting a non-existent user")
  void testGetNonExistentUser() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> userService.getUserById(nonExistentId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when updating a non-existent user")
  void testUpdateNonExistentUser() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    UserDto updateDto =
        TestDataFactory.createUserDto(nonExistentId, "Updated User", "updated@example.com", null);

    // When/Then
    assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateDto))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when deleting a non-existent user")
  void testDeleteNonExistentUser() {
    // Given
    UUID nonExistentId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when adding a calendar to a non-existent user")
  void testAddCalendarToNonExistentUser() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> userService.addCalendarToUser(nonExistentId, calendarId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName(
      "Should throw UserNotFoundException when removing a calendar from a non-existent user")
  void testRemoveCalendarFromNonExistentUser() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> userService.removeCalendarFromUser(nonExistentId, calendarId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with id: " + nonExistentId);
  }

  @Test
  @DisplayName(
      "Should throw DataIntegrityViolationException when creating a user with duplicate email")
  void testCreateUserWithDuplicateEmail() {
    // Given
    String email = "duplicate@example.com";
    UserDto firstUser = TestDataFactory.createUserDto("First User", email);
    userService.createUser(firstUser);

    UserDto duplicateUser = TestDataFactory.createUserDto("Duplicate User", email);

    // When/Then
    assertThatThrownBy(() -> userService.createUser(duplicateUser))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Should not add duplicate calendar to user")
  void testAddDuplicateCalendarToUser() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar User", "calendar@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();
    UUID calendarId = UUID.randomUUID();

    // When - Add calendar first time
    UserDto updatedUser1 = userService.addCalendarToUser(userId, calendarId);

    // Then
    assertThat(updatedUser1.getCalendarIds()).contains(calendarId);

    // When/Then - Add same calendar second time should throw exception
    assertThrows(
        CalendarAlreadyExistsException.class,
        () -> userService.addCalendarToUser(userId, calendarId));
  }

  @Test
  @DisplayName(
      "Should throw CalendarNotFoundException when removing a non-existent calendar from user")
  void testRemoveNonExistentCalendarFromUser() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar User", "calendar@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();
    UUID nonExistentCalendarId = UUID.randomUUID();

    // When/Then
    assertThatThrownBy(() -> userService.removeCalendarFromUser(userId, nonExistentCalendarId))
        .isInstanceOf(CalendarNotFoundException.class)
        .hasMessageContaining("Calendar not found");
  }
}
