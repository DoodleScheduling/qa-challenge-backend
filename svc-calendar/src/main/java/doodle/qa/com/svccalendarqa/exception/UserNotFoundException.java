package doodle.qa.com.svccalendarqa.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(UUID userId) {
    super("User not found with id: " + userId);
  }
}
