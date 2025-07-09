package doodle.qa.com.svcproviderqa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

  @NotBlank(message = "Name is required")
  private String name;

  private String description;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @JsonIgnore private Long version;

  @Valid @Builder.Default private List<EventDto> events = new ArrayList<>();
}
