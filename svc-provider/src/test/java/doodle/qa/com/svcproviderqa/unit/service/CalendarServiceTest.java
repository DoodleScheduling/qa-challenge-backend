package doodle.qa.com.svcproviderqa.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.entity.Calendar;
import doodle.qa.com.svcproviderqa.exception.CalendarNotFoundException;
import doodle.qa.com.svcproviderqa.exception.ConcurrentModificationException;
import doodle.qa.com.svcproviderqa.repository.CalendarRepository;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Unit tests for the CalendarService. These tests verify the business logic in the service layer
 * using mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

  @Mock private CalendarRepository calendarRepository;

  private CalendarService calendarService;

  @BeforeEach
  void setUp() {
    calendarService = new CalendarService(calendarRepository);
  }

  @Test
  @DisplayName("Should return all calendars when getting all calendars")
  void getAllCalendars_ShouldReturnAllCalendars() {
    // Given
    List<Calendar> calendars = TestDataFactory.createCalendarList(3);
    when(calendarRepository.findAll()).thenReturn(calendars);

    // When
    List<CalendarDto> result = calendarService.getAllCalendars();

    // Then
    assertThat(result).hasSize(3);
    verify(calendarRepository).findAll();
  }

  @Test
  @DisplayName("Should return calendar by ID when calendar exists")
  void getCalendarById_WhenCalendarExists_ShouldReturnCalendar() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar calendar =
        TestDataFactory.createCalendar(
            calendarId, "Test Calendar", "Test Description", new ArrayList<>());
    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));

    // When
    CalendarDto result = calendarService.getCalendarById(calendarId);

    // Then
    assertThat(result.getId()).isEqualTo(calendarId);
    assertThat(result.getName()).isEqualTo("Test Calendar");
    assertThat(result.getDescription()).isEqualTo("Test Description");
    verify(calendarRepository).findById(calendarId);
  }

  @Test
  @DisplayName(
      "Should throw CalendarNotFoundException when getting calendar by ID that doesn't exist")
  void getCalendarById_WhenCalendarDoesNotExist_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    when(calendarRepository.findById(calendarId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        CalendarNotFoundException.class, () -> calendarService.getCalendarById(calendarId));
    verify(calendarRepository).findById(calendarId);
  }

  @Test
  @DisplayName("Should create calendar when creating calendar")
  void createCalendar_ShouldCreateCalendar() {
    // Given
    CalendarDto calendarDto = TestDataFactory.createCalendarDto("New Calendar", "New Description");
    Calendar savedCalendar =
        TestDataFactory.createCalendar(
            UUID.randomUUID(), "New Calendar", "New Description", new ArrayList<>());
    when(calendarRepository.save(any(Calendar.class))).thenReturn(savedCalendar);

    // When
    CalendarDto result = calendarService.createCalendar(calendarDto);

    // Then
    assertThat(result.getId()).isEqualTo(savedCalendar.getId());
    assertThat(result.getName()).isEqualTo("New Calendar");
    assertThat(result.getDescription()).isEqualTo("New Description");

    ArgumentCaptor<Calendar> calendarCaptor = ArgumentCaptor.forClass(Calendar.class);
    verify(calendarRepository).save(calendarCaptor.capture());
    Calendar capturedCalendar = calendarCaptor.getValue();
    assertThat(capturedCalendar.getName()).isEqualTo("New Calendar");
    assertThat(capturedCalendar.getDescription()).isEqualTo("New Description");
  }

  @Test
  @DisplayName("Should update calendar when updating calendar that exists")
  void updateCalendar_WhenCalendarExists_ShouldUpdateCalendar() {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description");
    Calendar existingCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Original Calendar", "Original Description", new ArrayList<>());
    Calendar updatedCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Updated Calendar", "Updated Description", new ArrayList<>());

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(existingCalendar));
    when(calendarRepository.save(any(Calendar.class))).thenReturn(updatedCalendar);

    // When
    CalendarDto result = calendarService.updateCalendar(calendarId, calendarDto);

    // Then
    assertThat(result.getId()).isEqualTo(calendarId);
    assertThat(result.getName()).isEqualTo("Updated Calendar");
    assertThat(result.getDescription()).isEqualTo("Updated Description");

    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository).save(existingCalendar);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when updating calendar that doesn't exist")
  void updateCalendar_WhenCalendarDoesNotExist_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description");
    when(calendarRepository.findById(calendarId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(
        CalendarNotFoundException.class,
        () -> calendarService.updateCalendar(calendarId, calendarDto));
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository, never()).save(any(Calendar.class));
  }

  @Test
  @DisplayName("Should throw ConcurrentModificationException when version mismatch")
  void updateCalendar_WhenVersionMismatch_ShouldThrowConcurrentModificationException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(
            calendarId, "Updated Calendar", "Updated Description", 2L);
    Calendar existingCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Original Calendar", "Original Description", new ArrayList<>(), 1L);

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(existingCalendar));

    // When/Then
    assertThrows(
        ConcurrentModificationException.class,
        () -> calendarService.updateCalendar(calendarId, calendarDto));
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository, never()).save(any(Calendar.class));
  }

  @Test
  @DisplayName("Should delete calendar when deleting calendar that exists")
  void deleteCalendar_WhenCalendarExists_ShouldDeleteCalendar() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar existingCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Calendar To Delete", "Delete Description", new ArrayList<>());
    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(existingCalendar));

    // When
    calendarService.deleteCalendar(calendarId);

    // Then
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository).delete(existingCalendar);
  }

  @Test
  @DisplayName("Should throw CalendarNotFoundException when deleting calendar that doesn't exist")
  void deleteCalendar_WhenCalendarDoesNotExist_ShouldThrowCalendarNotFoundException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    when(calendarRepository.findById(calendarId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(CalendarNotFoundException.class, () -> calendarService.deleteCalendar(calendarId));
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository, never()).delete(any(Calendar.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when creating calendar")
  void createCalendar_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    CalendarDto calendarDto = TestDataFactory.createCalendarDto("New Calendar", "New Description");
    when(calendarRepository.save(any(Calendar.class)))
        .thenThrow(OptimisticLockingFailureException.class);

    // When/Then
    assertThrows(
        ConcurrentModificationException.class, () -> calendarService.createCalendar(calendarDto));
    verify(calendarRepository).save(any(Calendar.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when updating calendar")
  void updateCalendar_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(calendarId, "Updated Calendar", "Updated Description");
    Calendar existingCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Original Calendar", "Original Description", new ArrayList<>());

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(existingCalendar));
    when(calendarRepository.save(any(Calendar.class)))
        .thenThrow(OptimisticLockingFailureException.class);

    // When/Then
    assertThrows(
        ConcurrentModificationException.class,
        () -> calendarService.updateCalendar(calendarId, calendarDto));
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository).save(any(Calendar.class));
  }

  @Test
  @DisplayName("Should handle OptimisticLockingFailureException when deleting calendar")
  void deleteCalendar_WhenOptimisticLockingFailure_ShouldThrowConcurrentModificationException() {
    // Given
    UUID calendarId = UUID.randomUUID();
    Calendar existingCalendar =
        TestDataFactory.createCalendar(
            calendarId, "Calendar To Delete", "Delete Description", new ArrayList<>());

    when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(existingCalendar));
    doThrow(OptimisticLockingFailureException.class)
        .when(calendarRepository)
        .delete(any(Calendar.class));

    // When/Then
    assertThrows(
        ConcurrentModificationException.class, () -> calendarService.deleteCalendar(calendarId));
    verify(calendarRepository).findById(calendarId);
    verify(calendarRepository).delete(any(Calendar.class));
  }
}
