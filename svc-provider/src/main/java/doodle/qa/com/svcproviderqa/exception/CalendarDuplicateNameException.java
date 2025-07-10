package doodle.qa.com.svcproviderqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CalendarDuplicateNameException extends RuntimeException {

  public CalendarDuplicateNameException(String name) {
    super("Calendar with name '" + name + "' already exists");
  }

  public CalendarDuplicateNameException(String message, Throwable cause) {
    super(message, cause);
  }
}