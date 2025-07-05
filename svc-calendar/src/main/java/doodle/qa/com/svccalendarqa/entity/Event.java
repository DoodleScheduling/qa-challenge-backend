package doodle.qa.com.svccalendarqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Event entity representing an event within a calendar. Includes optimistic locking with version
 * field to handle concurrent modifications.
 */
@Entity
@Table(name = "events")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Event title is required")
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

  private String location;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_id", nullable = false)
  private Calendar calendar;

  @Version private Long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Calendar getCalendar() {
    return calendar;
  }

  public void setCalendar(Calendar calendar) {
    this.calendar = calendar;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Event)) return false;
    Event event = (Event) o;
    return id != null && id.equals(event.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Event{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", location='"
        + location
        + '\''
        + ", version="
        + version
        + '}';
  }
}
