package doodle.qa.com.svcproviderqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentModificationException extends RuntimeException {

  public ConcurrentModificationException(String message) {
    super(message);
  }

  public ConcurrentModificationException(String message, Throwable cause) {
    super(message, cause);
  }
}