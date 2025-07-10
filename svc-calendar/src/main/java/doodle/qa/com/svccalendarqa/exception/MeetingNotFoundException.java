package doodle.qa.com.svccalendarqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when a meeting is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MeetingNotFoundException extends RuntimeException {

    public MeetingNotFoundException(String message) {
        super(message);
    }

    public MeetingNotFoundException(UUID meetingId) {
        super("Meeting not found with ID: " + meetingId);
    }

    public MeetingNotFoundException(UUID meetingId, UUID userId) {
        super("Meeting not found with ID: " + meetingId + " for user ID: " + userId);
    }

    public MeetingNotFoundException(UUID meetingId, UUID userId, UUID calendarId) {
        super("Meeting not found with ID: " + meetingId + " for user ID: " + userId + " and calendar ID: " + calendarId);
    }
}
