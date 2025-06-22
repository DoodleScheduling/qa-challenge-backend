package doodle.qa.com.svcuserqa.exception;

/** Exception thrown when attempting to remove a calendar that doesn't exist for a user. */
public class CalendarNotFoundException extends RuntimeException {

  public CalendarNotFoundException(String message) {
    super(message);
  }
}
