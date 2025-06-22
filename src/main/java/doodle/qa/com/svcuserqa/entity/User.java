package doodle.qa.com.svcuserqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity representing a user in the system. Includes optimistic locking with version field to
 * handle concurrent modifications.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Name is required")
  @Column(nullable = false)
  private String name;

  @Email(message = "Email should be valid")
  @NotBlank(message = "Email is required")
  @Column(nullable = false, unique = true)
  private String email;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @Version private Long version;

  @ElementCollection
  @CollectionTable(name = "user_calendars", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "calendar_id")
  @Builder.Default
  private List<UUID> calendarIds = new ArrayList<>();
}
