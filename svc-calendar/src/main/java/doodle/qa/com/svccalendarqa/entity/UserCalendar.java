package doodle.qa.com.svccalendarqa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * UserCalendar entity representing a calendar associated with a user.
 * The id is the primary key, and calendar_id is referenced by the Meeting entity.
 */
@Entity
@Table(name = "user_calendars")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCalendar {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "calendar_id", nullable = false)
    private UUID calendarId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
