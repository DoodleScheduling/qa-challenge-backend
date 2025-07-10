package doodle.qa.com.svcproviderqa.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the CalendarService. These tests verify the integration between the service
 * layer and repository layer, ensuring that calendar operations correctly persist data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CalendarServiceIntegrationTest {

  @Autowired private CalendarService calendarService;

  @Autowired private CalendarRepository calendarRepository;

  @BeforeEach
  void setUp() {
    calendarRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    calendarRepository.deleteAll();
  }

  @Test
  @DisplayName("Should successfully create a calendar and persist it in the database")
  void testCreateCalendar() {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(
            "Integration Test Calendar", "Integration Test Description");

    // When
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);

    // Then
    assertThat(createdCalendar.getId()).isNotNull();
    assertThat(createdCalendar.getName()).isEqualTo("Integration Test Calendar");
    assertThat(createdCalendar.getDescription()).isEqualTo("Integration Test Description");

    Calendar savedCalendar = calendarRepository.findById(createdCalendar.getId()).orElseThrow();
    assertThat(savedCalendar.getName()).isEqualTo("Integration Test Calendar");
    assertThat(savedCalendar.getDescription()).isEqualTo("Integration Test Description");
  }

  @Test
  @DisplayName("Should successfully update a calendar's information")
  void testUpdateCalendar() {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Original Calendar", "Original Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    UUID calendarId = createdCalendar.getId();

    CalendarDto updateDto =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description");
    updateDto.setVersion(createdCalendar.getVersion()); // Set the version for optimistic locking

    // When
    CalendarDto updatedCalendar = calendarService.updateCalendar(calendarId, updateDto);

    // Then
    assertThat(updatedCalendar.getName()).isEqualTo("Updated Calendar");
    assertThat(updatedCalendar.getDescription()).isEqualTo("Updated Description");

    Calendar savedCalendar = calendarRepository.findById(calendarId).orElseThrow();
    assertThat(savedCalendar.getName()).isEqualTo("Updated Calendar");
    assertThat(savedCalendar.getDescription()).isEqualTo("Updated Description");
  }

  @Test
  @DisplayName("Should successfully delete a calendar from the database")
  void testDeleteCalendar() {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Calendar To Delete", "Delete Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    UUID calendarId = createdCalendar.getId();

    // When
    calendarService.deleteCalendar(calendarId);

    // Then
    assertThat(calendarRepository.findById(calendarId)).isEmpty();
  }

  @Test
  @DisplayName("Should successfully retrieve all calendars")
  void testGetAllCalendars() {
    // Given
    int totalCalendars = 5;
    for (int i = 0; i < totalCalendars; i++) {
      calendarService.createCalendar(
          TestDataFactory.createCalendarDto("Calendar " + i, "Description " + i));
    }

    // When
    List<CalendarDto> calendars = calendarService.getAllCalendars();

    // Then
    assertThat(calendars).hasSize(totalCalendars);
    for (int i = 0; i < totalCalendars; i++) {
      final int index = i;
      assertThat(calendars).anyMatch(c -> c.getName().equals("Calendar " + index));
    }
  }

  @Test
  @DisplayName("Should successfully retrieve a calendar by ID")
  void testGetCalendarById() {
    // Given
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto("Test Calendar", "Test Description");
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    UUID calendarId = createdCalendar.getId();

    // When
    CalendarDto retrievedCalendar = calendarService.getCalendarById(calendarId);

    // Then
    assertThat(retrievedCalendar.getId()).isEqualTo(calendarId);
    assertThat(retrievedCalendar.getName()).isEqualTo("Test Calendar");
    assertThat(retrievedCalendar.getDescription()).isEqualTo("Test Description");
  }
}
