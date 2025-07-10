package doodle.qa.com.svccalendarqa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for available time slots.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationMinutes;
}