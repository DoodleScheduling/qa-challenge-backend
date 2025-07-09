package doodle.qa.com.svcproviderqa.controller;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Calendar API", description = "API for calendar management")
public class CalendarController {

  private final CalendarService calendarService;

  /**
   * Retrieves all calendars with pagination.
   *
   * @param page Page number (zero-based, optional)
   * @param size Number of calendars per page (optional)
   * @return Simplified response with calendars and minimal pagination information
   */
  @GetMapping
  @Operation(
      summary = "Get all calendars",
      description =
          "Retrieves a list of all calendars with pagination. Use page and size parameters for pagination.")
  @ApiResponse(responseCode = "200", description = "Calendars retrieved successfully")
  public ResponseEntity<Map<String, Object>> getAllCalendars(
      @Parameter(description = "Page number (0-indexed)")
          @RequestParam(required = false, defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20")
          Integer size) {

    Pageable pageable = PageRequest.of(page, size);
    log.debug(
        "GET request to retrieve all calendars with pagination: page={}, size={}", page, size);

    Page<CalendarDto> calendarsPage = calendarService.getAllCalendars(pageable);

    // Create a simplified response with minimal pagination information
    Map<String, Object> response = new HashMap<>();
    response.put("calendars", calendarsPage.getContent());
    response.put("totalPages", calendarsPage.getTotalPages());
    response.put("currentPage", calendarsPage.getNumber());

    log.info(
        "Retrieved {} calendars (page {} of {})",
        calendarsPage.getNumberOfElements(),
        calendarsPage.getNumber() + 1,
        calendarsPage.getTotalPages());

    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a calendar by ID.
   *
   * @param id The calendar ID
   * @return The calendar with the specified ID
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get calendar by ID", description = "Retrieves a calendar by its ID")
  @ApiResponse(responseCode = "200", description = "Calendar retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Calendar not found", content = @Content)
  public ResponseEntity<CalendarDto> getCalendarById(
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID id) {
    log.debug("GET request to retrieve calendar with id: {}", id);
    CalendarDto calendar = calendarService.getCalendarById(id);
    log.info("Retrieved calendar with id: {}", id);
    return ResponseEntity.ok(calendar);
  }

  /**
   * Creates a new calendar.
   *
   * @param calendarDto The calendar data
   * @return The created calendar
   */
  @PostMapping
  @Operation(summary = "Create calendar", description = "Creates a new calendar")
  @ApiResponse(responseCode = "201", description = "Calendar created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<CalendarDto> createCalendar(
      @Parameter(description = "Calendar data", required = true) @Valid @RequestBody
          CalendarDto calendarDto) {
    log.debug("POST request to create calendar with name: {}", calendarDto.getName());
    CalendarDto createdCalendar = calendarService.createCalendar(calendarDto);
    log.info("Created calendar with id: {}", createdCalendar.getId());
    return new ResponseEntity<>(createdCalendar, HttpStatus.CREATED);
  }

  /**
   * Updates an existing calendar.
   *
   * @param id The calendar ID
   * @param calendarDto The updated calendar data
   * @return The updated calendar
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update calendar", description = "Updates an existing calendar")
  @ApiResponse(responseCode = "200", description = "Calendar updated successfully")
  @ApiResponse(responseCode = "404", description = "Calendar not found", content = @Content)
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<CalendarDto> updateCalendar(
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Calendar data", required = true) @Valid @RequestBody
          CalendarDto calendarDto) {
    log.debug("PUT request to update calendar with id: {}", id);
    CalendarDto updatedCalendar = calendarService.updateCalendar(id, calendarDto);
    log.info("Updated calendar with id: {}", id);
    return ResponseEntity.ok(updatedCalendar);
  }

  /**
   * Deletes a calendar.
   *
   * @param id The calendar ID
   * @return No content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete calendar", description = "Deletes a calendar")
  @ApiResponse(responseCode = "204", description = "Calendar deleted successfully")
  @ApiResponse(responseCode = "404", description = "Calendar not found", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<Void> deleteCalendar(
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID id) {
    log.debug("DELETE request to delete calendar with id: {}", id);
    calendarService.deleteCalendar(id);
    log.info("Deleted calendar with id: {}", id);
    return ResponseEntity.noContent().build();
  }
}
