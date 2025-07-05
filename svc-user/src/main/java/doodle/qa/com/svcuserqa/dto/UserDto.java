package doodle.qa.com.svcuserqa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
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
public class UserDto {

  private UUID id;

  @NotBlank(message = "Name is required")
  private String name;

  @Email(message = "Email should be valid")
  @NotBlank(message = "Email is required")
  private String email;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @JsonIgnore private Long version;

  @Builder.Default private List<UUID> calendarIds = new ArrayList<>();
}
