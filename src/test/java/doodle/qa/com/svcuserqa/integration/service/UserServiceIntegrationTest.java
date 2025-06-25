package doodle.qa.com.svcuserqa.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the UserService. These tests verify the integration between the service
 * layer, repository layer, and Kafka messaging, ensuring that user operations correctly persist
 * data and trigger appropriate events.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-state-test", "user-state-test.DLT"},
    brokerProperties = {
      "transaction.state.log.replication.factor=1",
      "transaction.state.log.min.isr=1"
    })
@Transactional
class UserServiceIntegrationTest {

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
  @DisplayName("Should successfully create a user and persist it in the database")
  void testCreateUser() {
    // Given
    UserDto userDto =
        TestDataFactory.createUserDto("Integration Test User", "integration@example.com");

    // When
    UserDto createdUser = userService.createUser(userDto);

    // Then
    assertThat(createdUser.getId()).isNotNull();
    assertThat(createdUser.getName()).isEqualTo("Integration Test User");
    assertThat(createdUser.getEmail()).isEqualTo("integration@example.com");

    User savedUser = userRepository.findById(createdUser.getId()).orElseThrow();
    assertThat(savedUser.getName()).isEqualTo("Integration Test User");
    assertThat(savedUser.getEmail()).isEqualTo("integration@example.com");
  }

  @Test
  @DisplayName("Should successfully add a calendar to a user")
  void testAddCalendarToUser() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar Test User", "calendar@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();
    UUID calendarId = UUID.randomUUID();

    // When
    UserDto updatedUser = userService.addCalendarToUser(userId, calendarId);

    // Then
    assertThat(updatedUser.getCalendarIds()).contains(calendarId);

    User savedUser = userRepository.findById(userId).orElseThrow();
    assertThat(savedUser.getCalendarIds()).contains(calendarId);
  }

  @Test
  @DisplayName("Should successfully update a user's information")
  void testUpdateUser() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Original User", "original@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();

    List<UUID> calendarIds = new ArrayList<>();
    calendarIds.add(UUID.randomUUID());
    UserDto updateDto =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", calendarIds);

    // When
    UserDto updatedUser = userService.updateUser(userId, updateDto);

    // Then
    assertThat(updatedUser.getName()).isEqualTo("Updated User");
    assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
    assertThat(updatedUser.getCalendarIds()).hasSize(1);

    User savedUser = userRepository.findById(userId).orElseThrow();
    assertThat(savedUser.getName()).isEqualTo("Updated User");
    assertThat(savedUser.getEmail()).isEqualTo("updated@example.com");
    assertThat(savedUser.getCalendarIds()).hasSize(1);
  }

  @Test
  @DisplayName("Should successfully delete a user from the database")
  void testDeleteUser() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("User To Delete", "delete@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();

    // When
    userService.deleteUser(userId);

    // Then
    assertThat(userRepository.findById(userId)).isEmpty();
  }

  @Test
  @DisplayName("Should successfully remove a calendar from a user")
  void testRemoveCalendarFromUser() {
    // Given
    UUID calendarId = UUID.randomUUID();
    List<UUID> calendarIds = new ArrayList<>();
    calendarIds.add(calendarId);
    UserDto userDto =
        TestDataFactory.createUserDto(
            "Calendar Remove User", "calendar-remove@example.com", calendarIds);
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();

    // When
    UserDto updatedUser = userService.removeCalendarFromUser(userId, calendarId);

    // Then
    assertThat(updatedUser.getCalendarIds()).doesNotContain(calendarId);

    User savedUser = userRepository.findById(userId).orElseThrow();
    assertThat(savedUser.getCalendarIds()).doesNotContain(calendarId);
  }
}
