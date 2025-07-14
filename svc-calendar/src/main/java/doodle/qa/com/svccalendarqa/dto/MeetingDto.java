package doodle.qa.com.svccalendarqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for Meeting entity. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDto {
  private UUID id;

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  private LocalDateTime endTime;

  private String location;

  @NotNull(message = "Calendar ID is required")
  private UUID calendarId;
}
