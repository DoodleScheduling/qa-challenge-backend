package doodle.qa.com.svccalendarqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when a calendar is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CalendarNotFoundException extends RuntimeException {
    
    public CalendarNotFoundException(String message) {
        super(message);
    }
    
    public CalendarNotFoundException(UUID calendarId) {
        super("Calendar not found with ID: " + calendarId);
    }
    
    public CalendarNotFoundException(UUID calendarId, UUID userId) {
        super("Calendar not found with ID: " + calendarId + " for user ID: " + userId);
    }
}