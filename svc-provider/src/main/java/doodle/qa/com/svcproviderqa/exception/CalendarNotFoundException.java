package doodle.qa.com.svcproviderqa.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CalendarNotFoundException extends RuntimeException {

  public CalendarNotFoundException(UUID id) {
    super("Calendar not found with id: " + id);
  }

  public CalendarNotFoundException(String message) {
    super(message);
  }
}