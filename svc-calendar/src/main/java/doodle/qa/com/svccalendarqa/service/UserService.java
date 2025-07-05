package doodle.qa.com.svccalendarqa.service;

import doodle.qa.com.svccalendarqa.entity.User;
import doodle.qa.com.svccalendarqa.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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

  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public void createOrUpdateUser(
      @NotNull UUID userId, @NotNull String name, @NotNull String email) {
    log.debug("Creating or updating user: {} with email: {}", userId, email);

    try {
      Optional<User> existingUser = userRepository.findById(userId);

      if (existingUser.isPresent()) {
        User user = existingUser.get();
        user.setName(name);
        user.setEmail(email);
        userRepository.save(user);
        log.info("User updated: {}", userId);
      } else {
        User user = User.builder().id(userId).name(name).email(email).build();
        userRepository.save(user);
        log.info("User created: {}", userId);
      }
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while creating/updating user: {}", userId, e);
      throw e;
    }
  }

  @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
  @Retryable(
      value = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 500, multiplier = 2))
  public void deleteUser(@NotNull UUID userId) {
    log.debug("Deleting user: {}", userId);

    try {
      Optional<User> existingUser = userRepository.findById(userId);
      if (existingUser.isPresent()) {
        userRepository.delete(existingUser.get());
        log.info("User deleted: {}", userId);
      } else {
        log.warn("Attempted to delete non-existent user: {}", userId);
      }
    } catch (OptimisticLockingFailureException e) {
      log.warn("Concurrent modification detected while deleting user: {}", userId, e);
      throw e;
    }
  }

  public boolean userExists(@NotNull UUID userId) {
    return userRepository.existsById(userId);
  }
}
