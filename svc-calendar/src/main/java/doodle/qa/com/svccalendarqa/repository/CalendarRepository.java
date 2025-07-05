package doodle.qa.com.svccalendarqa.repository;

import doodle.qa.com.svccalendarqa.entity.Calendar;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

  List<Calendar> findByOwnerId(UUID ownerId);

  @Query("SELECT c FROM Calendar c WHERE c.ownerId = :ownerId ORDER BY c.name")
  List<Calendar> findByOwnerIdOrderByName(@Param("ownerId") UUID ownerId);

  boolean existsByOwnerIdAndName(UUID ownerId, String name);
}
