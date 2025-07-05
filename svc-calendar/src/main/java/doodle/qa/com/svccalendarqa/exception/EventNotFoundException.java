package doodle.qa.com.svccalendarqa.exception;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

  public EventNotFoundException(String message) {
    super(message);
  }

  public EventNotFoundException(UUID eventId) {
    super("Event not found with id: " + eventId);
  }
}
