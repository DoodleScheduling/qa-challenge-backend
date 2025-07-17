package doodle.qa.com.svcproviderqa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import doodle.qa.com.svcproviderqa.validation.ValidEventTime;
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
@ValidEventTime(message = "End time must be after start time")
public class EventDto {
  private UUID id;

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  private LocalDateTime endTime;

  private String location;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @JsonIgnore private Long version;

  @NotNull(message = "Calendar ID is required")
  private UUID calendarId;
}
