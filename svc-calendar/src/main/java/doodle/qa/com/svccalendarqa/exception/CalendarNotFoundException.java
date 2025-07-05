package doodle.qa.com.svccalendarqa.exception;

import java.util.UUID;

public class CalendarNotFoundException extends RuntimeException {

  public CalendarNotFoundException(String message) {
    super(message);
  }

  public CalendarNotFoundException(UUID calendarId) {
    super("Calendar not found with id: " + calendarId);
  }
}
