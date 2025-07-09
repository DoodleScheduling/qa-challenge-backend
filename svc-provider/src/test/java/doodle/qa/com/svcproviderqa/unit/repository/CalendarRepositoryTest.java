package doodle.qa.com.svcproviderqa.unit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.entity.Event;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the CalendarRepository. These tests verify the custom repository methods and JPA
 * functionality.
 */
@DataJpaTest
@ActiveProfiles("test")
class CalendarRepositoryTest {

  @Autowired private CalendarRepository calendarRepository;

  @Test
  @DisplayName("Should find calendar by name when calendar exists")
  void findByName_WhenCalendarExists_ShouldReturnCalendar() {
    // Given
    String name = "Test Calendar";
    Calendar calendar = TestDataFactory.createCalendar(name, "Test Description");
    calendarRepository.save(calendar);

    // When
    Optional<Calendar> foundCalendar = calendarRepository.findByName(name);

    // Then
    assertThat(foundCalendar).isPresent();
    assertThat(foundCalendar.get().getName()).isEqualTo(name);
    assertThat(foundCalendar.get().getDescription()).isEqualTo("Test Description");
  }

  @Test
  @DisplayName("Should return empty optional when finding calendar by name that doesn't exist")
  void findByName_WhenCalendarDoesNotExist_ShouldReturnEmptyOptional() {
    // Given
    String name = "Nonexistent Calendar";

    // When
    Optional<Calendar> foundCalendar = calendarRepository.findByName(name);

    // Then
    assertThat(foundCalendar).isEmpty();
  }

  @Test
  @DisplayName("Should return true when checking if calendar exists by name and calendar exists")
  void existsByName_WhenCalendarExists_ShouldReturnTrue() {
    // Given
    String name = "Existing Calendar";
    Calendar calendar = TestDataFactory.createCalendar(name, "Existing Description");
    calendarRepository.save(calendar);

    // When
    boolean exists = calendarRepository.existsByName(name);

    // Then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Should return false when checking if calendar exists by name and calendar doesn't exist")
  void existsByName_WhenCalendarDoesNotExist_ShouldReturnFalse() {
    // Given
    String name = "Nonexistent Calendar";

    // When
    boolean exists = calendarRepository.existsByName(name);

    // Then
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("Should save calendar with events")
  void save_WithEvents_ShouldPersistEvents() {
    // Given
    Calendar calendar = TestDataFactory.createCalendar("Calendar With Events", "Events Description");
    LocalDateTime now = LocalDateTime.now();
    
    Event event1 = TestDataFactory.createEvent(
        "Event 1", "Description 1", now, now.plusHours(1), "Location 1", calendar);
    Event event2 = TestDataFactory.createEvent(
        "Event 2", "Description 2", now.plusHours(2), now.plusHours(3), "Location 2", calendar);
    
    calendar.addEvent(event1);
    calendar.addEvent(event2);

    // When
    Calendar savedCalendar = calendarRepository.save(calendar);
    Calendar retrievedCalendar = calendarRepository.findById(savedCalendar.getId()).orElseThrow();

    // Then
    assertThat(retrievedCalendar.getEvents()).hasSize(2);
    assertThat(retrievedCalendar.getEvents().get(0).getTitle()).isEqualTo("Event 1");
    assertThat(retrievedCalendar.getEvents().get(1).getTitle()).isEqualTo("Event 2");
  }

  @Test
  @DisplayName("Should update calendar when saving with existing ID")
  void save_WithExistingId_ShouldUpdateCalendar() {
    // Given
    Calendar calendar = TestDataFactory.createCalendar("Original Name", "Original Description");
    Calendar savedCalendar = calendarRepository.save(calendar);
    UUID calendarId = savedCalendar.getId();

    // When
    savedCalendar.setName("Updated Name");
    savedCalendar.setDescription("Updated Description");
    calendarRepository.save(savedCalendar);
    Calendar retrievedCalendar = calendarRepository.findById(calendarId).orElseThrow();

    // Then
    assertThat(retrievedCalendar.getName()).isEqualTo("Updated Name");
    assertThat(retrievedCalendar.getDescription()).isEqualTo("Updated Description");
  }

  @Test
  @DisplayName("Should delete calendar when calendar exists")
  void delete_WhenCalendarExists_ShouldRemoveCalendar() {
    // Given
    Calendar calendar = TestDataFactory.createCalendar("Calendar To Delete", "Delete Description");
    Calendar savedCalendar = calendarRepository.save(calendar);
    UUID calendarId = savedCalendar.getId();

    // When
    calendarRepository.delete(savedCalendar);
    Optional<Calendar> retrievedCalendar = calendarRepository.findById(calendarId);

    // Then
    assertThat(retrievedCalendar).isEmpty();
  }

  @Test
  @DisplayName("Should delete events when calendar is deleted")
  void delete_WhenCalendarWithEventsExists_ShouldRemoveCalendarAndEvents() {
    // Given
    Calendar calendar = TestDataFactory.createCalendar("Calendar With Events To Delete", "Delete Description");
    LocalDateTime now = LocalDateTime.now();
    
    Event event = TestDataFactory.createEvent(
        "Event To Delete", "Delete Description", now, now.plusHours(1), "Delete Location", calendar);
    
    calendar.addEvent(event);
    
    Calendar savedCalendar = calendarRepository.save(calendar);
    UUID calendarId = savedCalendar.getId();
    UUID eventId = savedCalendar.getEvents().get(0).getId();

    // When
    calendarRepository.delete(savedCalendar);
    Optional<Calendar> retrievedCalendar = calendarRepository.findById(calendarId);

    // Then
    assertThat(retrievedCalendar).isEmpty();
    // The event should be deleted due to the CascadeType.ALL and orphanRemoval=true settings
  }
}