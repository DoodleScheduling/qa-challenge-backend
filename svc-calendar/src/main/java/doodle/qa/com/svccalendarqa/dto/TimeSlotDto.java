package doodle.qa.com.svccalendarqa.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for available time slots. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private int durationMinutes;
}
