package doodle.qa.com.svccalendarqa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDto {

  private UUID id;

  @NotBlank(message = "Calendar name is required")
  private String name;

  private String description;

  @NotNull(message = "Owner ID is required")
  private UUID ownerId;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @JsonIgnore private Long version;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @Builder.Default private List<EventDto> events = new ArrayList<>();
}
