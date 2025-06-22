package doodle.qa.com.svcuserqa.exception;

/** Exception thrown when attempting to add a calendar that is already associated with a user. */
public class CalendarAlreadyExistsException extends RuntimeException {
  public CalendarAlreadyExistsException(String message) {
    super(message);
  }
}
