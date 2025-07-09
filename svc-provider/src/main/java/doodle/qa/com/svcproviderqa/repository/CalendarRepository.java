package doodle.qa.com.svcproviderqa.repository;

import doodle.qa.com.svcproviderqa.entity.Calendar;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
  Optional<Calendar> findByName(String name);

  boolean existsByName(String name);
}
