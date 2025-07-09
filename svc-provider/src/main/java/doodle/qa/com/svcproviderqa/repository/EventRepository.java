package doodle.qa.com.svcproviderqa.repository;

import doodle.qa.com.svcproviderqa.entity.Event;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
  List<Event> findByCalendar(Calendar calendar);
  
  List<Event> findByCalendarId(UUID calendarId);
  
  List<Event> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
  
  List<Event> findByCalendarIdAndStartTimeBetween(UUID calendarId, LocalDateTime start, LocalDateTime end);
}