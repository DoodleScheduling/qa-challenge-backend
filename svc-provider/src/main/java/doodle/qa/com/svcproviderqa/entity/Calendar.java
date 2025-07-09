package doodle.qa.com.svcproviderqa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calendar entity representing a calendar in the system. Includes optimistic locking with version field to
 * handle concurrent modifications.
 */
@Entity
@Table(name = "calendars", uniqueConstraints = {
    @UniqueConstraint(columnNames = "name", name = "uk_calendar_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Name is required")
  @Column(nullable = false, unique = true)
  private String name;

  @Column(length = 1000)
  private String description;

  /**
   * Version field for optimistic locking. This helps prevent concurrent modifications by detecting
   * conflicts.
   */
  @Version 
  private Long version;

  @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Event> events = new ArrayList<>();

  /**
   * Helper method to add an event to the calendar
   */
  public void addEvent(Event event) {
    events.add(event);
    event.setCalendar(this);
  }

  /**
   * Helper method to remove an event from the calendar
   */
  public void removeEvent(Event event) {
    events.remove(event);
    event.setCalendar(null);
  }
}
