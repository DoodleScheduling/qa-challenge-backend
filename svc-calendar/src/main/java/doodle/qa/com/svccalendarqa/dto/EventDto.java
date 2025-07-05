package doodle.qa.com.svccalendarqa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

  private UUID id;

  @NotBlank(message = "Event title is required")
  private String title;

  private String description;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  private LocalDateTime endTime;

  private String location;

  @NotNull(message = "Calendar ID is required")
  private UUID calendarId;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @JsonIgnore private Long version;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
