package doodle.qa.com.svcuserqa.exception;

/**
 * Exception thrown when attempting to add a calendar when the user has reached the maximum limit.
 */
public class CalendarLimitExceededException extends RuntimeException {
  public CalendarLimitExceededException(String message) {
    super(message);
  }
}
