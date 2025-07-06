package doodle.qa.com.svcuserqa.exception;

/**
 * Exception thrown when a concurrent modification is detected. This happens when multiple users try
 * to modify the same entity simultaneously.
 */
public class ConcurrentModificationException extends RuntimeException {

  /**
   * Constructs a new ConcurrentModificationException with the specified detail message.
   *
   * @param message the detail message
   */
  public ConcurrentModificationException(String message) {
    super(message);
  }

  /**
   * Constructs a new ConcurrentModificationException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public ConcurrentModificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
