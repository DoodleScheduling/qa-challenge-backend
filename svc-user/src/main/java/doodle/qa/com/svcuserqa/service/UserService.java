package doodle.qa.com.svcuserqa.service;

import com.example.svcuser.avro.EventType;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.exception.CalendarAlreadyExistsException;
import doodle.qa.com.svcuserqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcuserqa.exception.ConcurrentModificationException;
import doodle.qa.com.svcuserqa.exception.UserNotFoundException;
import doodle.qa.com.svcuserqa.kafka.UserStateProducer;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserStateProducer userStateProducer;

  /**
   * Retrieves all users with pagination support.
   *
   * @param pageable Pagination information
   * @return Page of UserDto objects
   */
  public Page<UserDto> getAllUsers(Pageable pageable) {
    log.debug("Retrieving all users with pagination: {}", pageable);
    return userRepository.findAll(pageable).map(this::mapToDto);
  }

  /**
   * Retrieves all users. Note: For large datasets, consider using the paginated version.
   *
   * @return List of all UserDto objects
   */
  public List<UserDto> getAllUsers() {
    log.debug("Retrieving all users");
    return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
  }

  /**
   * Retrieves a user by ID.
   *
   * @param id The user ID
   * @return UserDto for the specified ID
   * @throws UserNotFoundException if user not found
   */
  public UserDto getUserById(@NotNull UUID id) {
    log.debug("Retrieving user with id: {}", id);
    return userRepository
        .findById(id)
        .map(this::mapToDto)
        .orElseThrow(
            () -> {
              log.warn("User not found with id: {}", id);
              return new UserNotFoundException(id);
            });
  }

  /**
   * Creates a new user.
   *
   * @param userDto The user data
   * @return UserDto for the created user
   * @throws ConcurrentModificationException if there's a conflict during creation
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public UserDto createUser(@NotNull @Valid UserDto userDto) {
    log.debug("Creating user with email: {}", userDto.getEmail());

    // Check if a user with the same email already exists
    if (userRepository.existsByEmail(userDto.getEmail())) {
      log.warn("User with email {} already exists", userDto.getEmail());
      throw new ConcurrentModificationException(
          "A user with this email already exists. Please use a different email.");
    }

    try {
      // Defensive copy of calendar IDs
      List<UUID> calendarIdsCopy =
          userDto.getCalendarIds() != null
              ? new ArrayList<>(userDto.getCalendarIds())
              : new ArrayList<>();

      User user =
          User.builder()
              .name(userDto.getName())
              .email(userDto.getEmail())
              .calendarIds(calendarIdsCopy)
              .build();

      User savedUser = userRepository.save(user);

      log.info("User created: {}", savedUser.getId());

      // Only send Kafka message if user creation was successful
      userStateProducer.sendUserState(savedUser, EventType.CREATED);

      return mapToDto(savedUser);
    } catch (OptimisticLockingFailureException e) {
      // This is unlikely for new entities but could happen in edge cases
      log.warn(
          "Concurrent modification detected while creating user with email: {}",
          userDto.getEmail(),
          e);
      throw new ConcurrentModificationException(
          "A conflict occurred while creating the user. Please try again.", e);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      // This happens when trying to create a user with a duplicate email
      log.warn(
          "Data integrity violation detected while creating user with email: {}",
          userDto.getEmail(),
          e);
      throw new ConcurrentModificationException(
          "A user with this email already exists. Please use a different email.", e);
    }
  }

  /**
   * Updates an existing user.
   *
   * @param id The user ID
   * @param userDto The updated user data
   * @return UserDto for the updated user
   * @throws UserNotFoundException if user not found
   * @throws ConcurrentModificationException if the user was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public UserDto updateUser(@NotNull UUID id, @NotNull @Valid UserDto userDto) {
    log.debug("Updating user with id: {}", id);

    try {
      User user =
          userRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("User not found with id: {}", id);
                    return new UserNotFoundException(id);
                  });

      // Defensive copy of calendar IDs
      List<UUID> calendarIdsCopy =
          userDto.getCalendarIds() != null
              ? new ArrayList<>(userDto.getCalendarIds())
              : new ArrayList<>();

      // Check if version matches to ensure optimistic locking
      if (userDto.getVersion() != null
          && !Objects.equals(user.getVersion(), userDto.getVersion())) {
        log.warn(
            "Version mismatch detected while updating user with id: {}. Expected: {}, Actual: {}",
            id,
            userDto.getVersion(),
            user.getVersion());
        throw new ConcurrentModificationException(
            "The user was modified by another operation. Please refresh and try again.");
      }

      user.setName(userDto.getName());
      user.setEmail(userDto.getEmail());
      user.setCalendarIds(calendarIdsCopy);

      User updatedUser = userRepository.save(user);

      log.info("User updated: {}", updatedUser.getId());

      // Only send Kafka message if user update was successful
      userStateProducer.sendUserState(updatedUser, EventType.UPDATED);

      return mapToDto(updatedUser);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while updating user with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The user was modified by another operation. Please refresh and try again.", e);
    } catch (Exception e) {
      log.error("Error occurred while updating user with id: {}", id, e);
      throw e;
    }
  }

  /**
   * Deletes a user.
   *
   * @param id The user ID
   * @throws UserNotFoundException if user not found
   * @throws ConcurrentModificationException if the user was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public void deleteUser(@NotNull UUID id) {
    log.debug("Deleting user with id: {}", id);

    try {
      User user =
          userRepository
              .findById(id)
              .orElseThrow(
                  () -> {
                    log.warn("User not found with id: {}", id);
                    return new UserNotFoundException(id);
                  });

      userRepository.delete(user);

      log.info("User deleted: {}", id);

      // Only send Kafka message if user deletion was successful
      userStateProducer.sendUserState(user, EventType.DELETED);
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while deleting user with id: {}", id, e);
      throw new ConcurrentModificationException(
          "The user was modified by another operation. Please refresh and try again.", e);
    } catch (Exception e) {
      log.error("Error occurred while deleting user with id: {}", id, e);
      throw e;
    }
  }

  /**
   * Adds a calendar to a user.
   *
   * @param userId The user ID
   * @param calendarId The calendar ID to add
   * @return UserDto for the updated user
   * @throws UserNotFoundException if user not found
   * @throws CalendarAlreadyExistsException if the calendar is already added to the user
   * @throws ConcurrentModificationException if the user was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public UserDto addCalendarToUser(@NotNull UUID userId, @NotNull UUID calendarId) {
    log.debug("Adding calendar {} to user {}", calendarId, userId);

    try {
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(
                  () -> {
                    log.warn("User not found with id: {}", userId);
                    return new UserNotFoundException(userId);
                  });

      // Check if calendar already exists and throw exception if it does
      if (user.getCalendarIds().contains(calendarId)) {
        log.warn("Calendar {} is already associated with user {}", calendarId, userId);
        throw new CalendarAlreadyExistsException("This calendar is already associated.");
      }

      user.getCalendarIds().add(calendarId);
      User savedUser = userRepository.save(user);

      log.info("Calendar {} added to user {}", calendarId, userId);

      // Only send Kafka message if calendar addition was successful
      userStateProducer.sendUserState(savedUser, EventType.CALENDAR_ADDED);

      return mapToDto(savedUser);
    } catch (OptimisticLockingFailureException e) {
      log.warn(
          "Concurrent modification detected while adding calendar {} to user {}",
          calendarId,
          userId,
          e);
      throw new ConcurrentModificationException(
          "The user was modified by another operation. Please refresh and try again.", e);
    } catch (Exception e) {
      log.error("Error occurred while adding calendar {} to user {}", calendarId, userId, e);
      throw e;
    }
  }

  /**
   * Removes a calendar from a user.
   *
   * @param userId The user ID
   * @param calendarId The calendar ID to remove
   * @return UserDto for the updated user
   * @throws UserNotFoundException if user not found
   * @throws CalendarNotFoundException if calendar not found for the user
   * @throws ConcurrentModificationException if the user was modified concurrently
   */
  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public UserDto removeCalendarFromUser(@NotNull UUID userId, @NotNull UUID calendarId) {
    log.debug("Removing calendar {} from user {}", calendarId, userId);

    try {
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(
                  () -> {
                    log.warn("User not found with id: {}", userId);
                    return new UserNotFoundException(userId);
                  });

      // Check if calendar exists and throw exception if it doesn't
      if (!user.getCalendarIds().contains(calendarId)) {
        log.warn("Calendar {} not found for user {}", calendarId, userId);
        throw new CalendarNotFoundException("Calendar not found.");
      }

      // Remove the calendar and save the user
      user.getCalendarIds().remove(calendarId);
      User savedUser = userRepository.save(user);

      log.info("Calendar {} removed from user {}", calendarId, userId);

      // Only send Kafka message if calendar removal was successful
      userStateProducer.sendUserState(savedUser, EventType.CALENDAR_REMOVED);

      return mapToDto(savedUser);
    } catch (OptimisticLockingFailureException e) {
      log.warn(
          "Concurrent modification detected while removing calendar {} from user {}",
          calendarId,
          userId,
          e);
      throw new ConcurrentModificationException(
          "The user was modified by another operation. Please refresh and try again.", e);
    } catch (Exception e) {
      log.error("Error occurred while removing calendar {} from user {}", calendarId, userId, e);
      throw e;
    }
  }

  /**
   * Maps a User entity to a UserDto. Creates a defensive copy of mutable collections to prevent
   * modification of the entity.
   *
   * @param user The User entity
   * @return UserDto with copied data
   */
  private UserDto mapToDto(User user) {
    if (user == null) {
      return null;
    }

    // Create a defensive copy of the calendar IDs
    List<UUID> calendarIdsCopy =
        user.getCalendarIds() != null ? new ArrayList<>(user.getCalendarIds()) : new ArrayList<>();

    return UserDto.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .version(user.getVersion())
        .calendarIds(calendarIdsCopy)
        .build();
  }
}
