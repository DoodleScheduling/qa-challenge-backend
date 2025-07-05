package doodle.qa.com.svccalendarqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Calendar entity representing a calendar owned by a user. Includes optimistic locking with version
 * field to handle concurrent modifications.
 */
@Entity
@Table(name = "calendars")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Calendar name is required")
  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Owner ID is required")
  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Event> events = new ArrayList<>();

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public List<Event> getEvents() {
    if (events == null) {
      events = new ArrayList<>();
    }
    return events;
  }

  public void setEvents(List<Event> events) {
    this.events = events;
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

  // Utility methods for managing bidirectional relationship
  public void addEvent(Event event) {
    getEvents().add(event);
    event.setCalendar(this);
  }

  public void removeEvent(Event event) {
    getEvents().remove(event);
    event.setCalendar(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Calendar)) return false;
    Calendar calendar = (Calendar) o;
    return id != null && id.equals(calendar.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Calendar{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", ownerId="
        + ownerId
        + ", version="
        + version
        + '}';
  }
}
