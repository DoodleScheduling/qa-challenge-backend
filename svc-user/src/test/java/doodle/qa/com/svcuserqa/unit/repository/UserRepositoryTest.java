package doodle.qa.com.svcuserqa.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the UserRepository. These tests verify the custom repository methods and JPA
 * functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("Should find user by email when user exists")
  void findByEmail_WhenUserExists_ShouldReturnUser() {
    // Given
    String email = "test@example.com";
    User user = TestDataFactory.createUser("Test User", email);
    userRepository.save(user);

    // When
    Optional<User> foundUser = userRepository.findByEmail(email);

    // Then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo(email);
    assertThat(foundUser.get().getName()).isEqualTo("Test User");
  }

  @Test
  @DisplayName("Should return empty optional when finding user by email that doesn't exist")
  void findByEmail_WhenUserDoesNotExist_ShouldReturnEmptyOptional() {
    // Given
    String email = "nonexistent@example.com";

    // When
    Optional<User> foundUser = userRepository.findByEmail(email);

    // Then
    assertThat(foundUser).isEmpty();
  }

  @Test
  @DisplayName("Should return true when checking if user exists by email and user exists")
  void existsByEmail_WhenUserExists_ShouldReturnTrue() {
    // Given
    String email = "exists@example.com";
    User user = TestDataFactory.createUser("Existing User", email);
    userRepository.save(user);

    // When
    boolean exists = userRepository.existsByEmail(email);

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Should return false when checking if user exists by email and user doesn't exist")
  void existsByEmail_WhenUserDoesNotExist_ShouldReturnFalse() {
    // Given
    String email = "nonexistent@example.com";

    // When
    boolean exists = userRepository.existsByEmail(email);

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("Should save user with calendar IDs")
  void save_WithCalendarIds_ShouldPersistCalendarIds() {
    // Given
    UUID calendarId1 = UUID.randomUUID();
    UUID calendarId2 = UUID.randomUUID();
    User user =
        TestDataFactory.createUser(
            "User With Calendars",
            "calendars@example.com",
            java.util.Arrays.asList(calendarId1, calendarId2));

    // When
    User savedUser = userRepository.save(user);
    User retrievedUser = userRepository.findById(savedUser.getId()).orElseThrow();

    // Then
    assertThat(retrievedUser.getCalendarIds()).hasSize(2);
    assertThat(retrievedUser.getCalendarIds()).contains(calendarId1, calendarId2);
  }

  @Test
  @DisplayName("Should update user when saving with existing ID")
  void save_WithExistingId_ShouldUpdateUser() {
    // Given
    User user = TestDataFactory.createUser("Original Name", "update@example.com");
    User savedUser = userRepository.save(user);
    UUID userId = savedUser.getId();

    // When
    savedUser.setName("Updated Name");
    userRepository.save(savedUser);
    User retrievedUser = userRepository.findById(userId).orElseThrow();

    // Then
    assertThat(retrievedUser.getName()).isEqualTo("Updated Name");
  }

  @Test
  @DisplayName("Should delete user when user exists")
  void delete_WhenUserExists_ShouldRemoveUser() {
    // Given
    User user = TestDataFactory.createUser("User To Delete", "delete@example.com");
    User savedUser = userRepository.save(user);
    UUID userId = savedUser.getId();

    // When
    userRepository.delete(savedUser);
    Optional<User> retrievedUser = userRepository.findById(userId);

    // Then
    assertThat(retrievedUser).isEmpty();
  }
}
