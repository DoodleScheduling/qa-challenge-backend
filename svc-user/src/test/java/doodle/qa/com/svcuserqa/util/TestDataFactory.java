package doodle.qa.com.svcuserqa.util;

import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects. This class provides methods to create test users
 * and user DTOs with predefined or random data.
 */
public class TestDataFactory {

  /**
   * Creates a User entity with the given ID, name, email, calendar IDs, and version.
   *
   * @param id The user ID
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @param version The version for optimistic locking
   * @return A User entity
   */
  public static User createUser(
      UUID id, String name, String email, List<UUID> calendarIds, Long version) {
    return User.builder()
        .id(id)
        .name(name)
        .email(email)
        .calendarIds(calendarIds != null ? calendarIds : new ArrayList<>())
        .version(version)
        .build();
  }

  /**
   * Creates a User entity with the given ID, name, email, and calendar IDs. The version will be
   * null, which is appropriate for new users that haven't been persisted yet.
   *
   * @param id The user ID
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @return A User entity
   */
  public static User createUser(UUID id, String name, String email, List<UUID> calendarIds) {
    return createUser(id, name, email, calendarIds, null);
  }

  /**
   * Creates a User entity with the given name, email, and calendar IDs. The ID will be null, which
   * is appropriate for new users that haven't been persisted yet.
   *
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @return A User entity
   */
  public static User createUser(String name, String email, List<UUID> calendarIds) {
    return createUser(null, name, email, calendarIds);
  }

  /**
   * Creates a User entity with the given name and email. The ID will be null and the calendar IDs
   * list will be empty.
   *
   * @param name The user name
   * @param email The user email
   * @return A User entity
   */
  public static User createUser(String name, String email) {
    return createUser(null, name, email, new ArrayList<>());
  }

  /**
   * Creates a UserDto with the given ID, name, email, calendar IDs, and version.
   *
   * @param id The user ID
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @param version The version for optimistic locking
   * @return A UserDto
   */
  public static UserDto createUserDto(
      UUID id, String name, String email, List<UUID> calendarIds, Long version) {
    return UserDto.builder()
        .id(id)
        .name(name)
        .email(email)
        .calendarIds(calendarIds != null ? calendarIds : new ArrayList<>())
        .version(version)
        .build();
  }

  /**
   * Creates a UserDto with the given ID, name, email, and calendar IDs. The version will be null,
   * which is appropriate for new users that haven't been persisted yet.
   *
   * @param id The user ID
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @return A UserDto
   */
  public static UserDto createUserDto(UUID id, String name, String email, List<UUID> calendarIds) {
    return createUserDto(id, name, email, calendarIds, null);
  }

  /**
   * Creates a UserDto with the given name, email, calendar IDs, and version. The ID will be null,
   * which is appropriate for new users that haven't been persisted yet.
   *
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @param version The version for optimistic locking
   * @return A UserDto
   */
  public static UserDto createUserDto(
      String name, String email, List<UUID> calendarIds, Long version) {
    return createUserDto(null, name, email, calendarIds, version);
  }

  /**
   * Creates a UserDto with the given name, email, and calendar IDs. The ID and version will be
   * null, which is appropriate for new users that haven't been persisted yet.
   *
   * @param name The user name
   * @param email The user email
   * @param calendarIds The list of calendar IDs
   * @return A UserDto
   */
  public static UserDto createUserDto(String name, String email, List<UUID> calendarIds) {
    return createUserDto(null, name, email, calendarIds, null);
  }

  /**
   * Creates a UserDto with the given name and email. The ID and version will be null, and the
   * calendar IDs list will be empty.
   *
   * @param name The user name
   * @param email The user email
   * @return A UserDto
   */
  public static UserDto createUserDto(String name, String email) {
    return createUserDto(null, name, email, new ArrayList<>(), null);
  }

  /**
   * Creates a list of User entities for testing with version information.
   *
   * @param count The number of users to create
   * @return A list of User entities
   */
  public static List<User> createUserList(int count) {
    List<User> users = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      users.add(
          createUser(
              UUID.randomUUID(),
              "User " + i,
              "user" + i + "@example.com",
              Arrays.asList(UUID.randomUUID(), UUID.randomUUID()),
              0L // Initialize with version 0
              ));
    }
    return users;
  }

  /**
   * Creates a list of UserDto objects for testing with version information.
   *
   * @param count The number of user DTOs to create
   * @return A list of UserDto objects
   */
  public static List<UserDto> createUserDtoList(int count) {
    List<UserDto> userDtos = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      userDtos.add(
          createUserDto(
              UUID.randomUUID(),
              "User " + i,
              "user" + i + "@example.com",
              Arrays.asList(UUID.randomUUID(), UUID.randomUUID()),
              0L // Initialize with version 0
              ));
    }
    return userDtos;
  }
}
