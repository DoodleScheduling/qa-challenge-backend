package doodle.qa.com.svcuserqa.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(), // Safe to expose since it's a domain-specific exception
            LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles ConcurrentModificationException. Returns a 409 Conflict status code with a generic
   * conflict message.
   *
   * @param ex the exception
   * @return the response entity with error details
   */
  @ExceptionHandler(ConcurrentModificationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ErrorResponse> handleConcurrentModificationException(
      ConcurrentModificationException ex) {
    String errorId = UUID.randomUUID().toString();
    // Log the full exception with errorId for tracking
    log.error("Error ID: {} - Concurrent modification error: {}", errorId, ex.getMessage(), ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "The resource was modified by another request. Please refresh and try again. (Error ID: "
                + errorId
                + ")",
            LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Handles OptimisticLockingFailureException. Returns a 409 Conflict status code with a generic
   * conflict message.
   *
   * @param ex the exception
   * @return the response entity with error details
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
      OptimisticLockingFailureException ex) {
    String errorId = UUID.randomUUID().toString();
    // Log the full exception with errorId for tracking
    log.error("Error ID: {} - Optimistic locking failure: {}", errorId, ex.getMessage(), ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "The record was updated by another user. Please refresh and try again. (Error ID: "
                + errorId
                + ")",
            LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Handles CalendarAlreadyExistsException. Returns a 409 Conflict status code.
   *
   * @param ex the exception
   * @return the response entity with error details
   */
  @ExceptionHandler(CalendarAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ResponseEntity<ErrorResponse> handleCalendarAlreadyExistsException(
      CalendarAlreadyExistsException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Handles CalendarNotFoundException. Returns a 404 Not Found status code.
   *
   * @param ex the exception
   * @return the response entity with error details
   */
  @ExceptionHandler(CalendarNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponse> handleCalendarNotFoundException(
      CalendarNotFoundException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
    String errorId = UUID.randomUUID().toString();
    // Log the full exception with stack trace and errorId
    log.error("Error ID: {} - Unexpected error occurred: {}", errorId, ex.getMessage(), ex);

    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred. Please contact support with this error ID: " + errorId,
            LocalDateTime.now());
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
