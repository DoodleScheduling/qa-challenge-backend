package doodle.qa.com.svccalendarqa.repository;

import doodle.qa.com.svccalendarqa.entity.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

  List<Event> findByCalendarId(UUID calendarId);

  @Query("SELECT e FROM Event e WHERE e.calendar.id = :calendarId ORDER BY e.startTime")
  List<Event> findByCalendarIdOrderByStartTime(@Param("calendarId") UUID calendarId);

  @Query(
      "SELECT e FROM Event e WHERE e.calendar.id = :calendarId AND e.startTime >= :startTime AND e.endTime <= :endTime ORDER BY e.startTime")
  List<Event> findByCalendarIdAndTimeRange(
      @Param("calendarId") UUID calendarId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  @Query("SELECT e FROM Event e WHERE e.calendar.ownerId = :ownerId ORDER BY e.startTime")
  List<Event> findByOwnerIdOrderByStartTime(@Param("ownerId") UUID ownerId);
}
