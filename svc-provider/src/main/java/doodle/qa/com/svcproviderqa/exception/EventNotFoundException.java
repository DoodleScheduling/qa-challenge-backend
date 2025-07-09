package doodle.qa.com.svcproviderqa.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventNotFoundException extends RuntimeException {

  public EventNotFoundException(UUID id) {
    super("Event not found with id: " + id);
  }

  public EventNotFoundException(String message) {
    super(message);
  }
}