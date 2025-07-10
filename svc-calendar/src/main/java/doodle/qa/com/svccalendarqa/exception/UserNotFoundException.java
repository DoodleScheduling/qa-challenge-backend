package doodle.qa.com.svccalendarqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when a user is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId);
    }
    
    public UserNotFoundException(String email, String field) {
        super("User not found with " + field + ": " + email);
    }
}