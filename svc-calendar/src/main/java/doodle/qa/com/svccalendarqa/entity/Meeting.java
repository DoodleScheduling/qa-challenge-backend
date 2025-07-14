package doodle.qa.com.svccalendarqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Meeting entity representing a meeting in the system. Includes optimistic locking with version
 * field to handle concurrent modifications.
 */
@Entity
@Table(name = "meetings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Title is required")
  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Start time is required")
  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column private String location;

  /**
   * Calendar ID for this meeting. This is a foreign key to the UserCalendar entity's id field. This
   * is the primary way to associate meetings with user calendars.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_calendar_id", referencedColumnName = "id", nullable = false)
  private UserCalendar userCalendar;

  /**
   * Calendar ID for this meeting. This is a duplicate of the UserCalendar entity's calendarId
   * field, stored for performance reasons.
   */
  @Column(name = "calendar_id", nullable = false)
  private UUID calendarId;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @Version private Long version;

  @NotNull(message = "Created at is required")
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @NotNull(message = "Updated at is required")
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
