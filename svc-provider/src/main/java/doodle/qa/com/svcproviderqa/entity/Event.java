package doodle.qa.com.svcproviderqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event entity representing an event in a calendar. Includes optimistic locking with version field to
 * handle concurrent modifications.
 */
@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Title is required")
  @Column(nullable = false)
  private String title;

  @Column(length = 1000)
  private String description;

  @NotNull(message = "Start time is required")
  @Column(nullable = false)
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  @Column(nullable = false)
  private LocalDateTime endTime;

  @Column
  private String location;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @Version 
  private Long version;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_id")
  @ToString.Exclude
  private Calendar calendar;
}