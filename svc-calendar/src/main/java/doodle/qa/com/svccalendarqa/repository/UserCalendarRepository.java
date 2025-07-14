package doodle.qa.com.svccalendarqa.repository;

import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for UserCalendar entity. */
@Repository
public interface UserCalendarRepository extends JpaRepository<UserCalendar, UUID> {

  /**
   * Find all calendars for a user.
   *
   * @param userId the user ID
   * @return a list of user calendars
   */
  List<UserCalendar> findByUserId(UUID userId);

  /**
   * Find a calendar by calendar ID and user ID.
   *
   * @param calendarId the calendar ID
   * @param userId the user ID
   * @return an optional user calendar
   */
  Optional<UserCalendar> findByCalendarIdAndUserId(UUID calendarId, UUID userId);

  /**
   * Check if a calendar exists for a user.
   *
   * @param calendarId the calendar ID
   * @param userId the user ID
   * @return true if the calendar exists for the user, false otherwise
   */
  boolean existsByCalendarIdAndUserId(UUID calendarId, UUID userId);

  /**
   * Find a calendar by calendar ID.
   *
   * @param calendarId the calendar ID
   * @return an optional user calendar
   * @deprecated Use findAllByCalendarId instead to support many-to-many relationships
   */
  @Deprecated
  Optional<UserCalendar> findByCalendarId(UUID calendarId);

  /**
   * Find all calendars by calendar ID.
   *
   * @param calendarId the calendar ID
   * @return a list of user calendars
   */
  List<UserCalendar> findAllByCalendarId(UUID calendarId);
}
