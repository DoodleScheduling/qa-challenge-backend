package doodle.qa.com.svcproviderqa.controller;

import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event API", description = "API for event management")
public class EventController {

  private final EventService eventService;


  /**
   * Retrieves an event by ID.
   *
   * @param id The event ID
   * @return The event with the specified ID
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get event by ID", description = "Retrieves an event by its ID")
  @ApiResponse(responseCode = "200", description = "Event retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
  public ResponseEntity<EventDto> getEventById(
      @Parameter(description = "Event ID", required = true) @PathVariable UUID id) {
    log.debug("GET request to retrieve event with id: {}", id);
    EventDto event = eventService.getEventById(id);
    log.info("Retrieved event with id: {}", id);
    return ResponseEntity.ok(event);
  }

  /**
   * Retrieves events by calendar ID.
   *
   * @param calendarId The calendar ID
   * @return List of events for the specified calendar
   */
  @GetMapping("/calendar/{calendarId}")
  @Operation(
      summary = "Get events by calendar ID",
      description = "Retrieves all events for a specific calendar")
  @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  public ResponseEntity<List<EventDto>> getEventsByCalendarId(
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID calendarId) {
    log.debug("GET request to retrieve events for calendar id: {}", calendarId);
    List<EventDto> events = eventService.getEventsByCalendarId(calendarId);
    log.info("Retrieved {} events for calendar id: {}", events.size(), calendarId);
    return ResponseEntity.ok(events);
  }


  /**
   * Retrieves events for a calendar within a time range.
   *
   * @param calendarId The calendar ID
   * @param start Start time
   * @param end End time
   * @return List of events for the specified calendar within the time range
   */
  @GetMapping("/calendar/{calendarId}/timerange")
  @Operation(
      summary = "Get events by calendar ID and time range",
      description = "Retrieves all events for a specific calendar within a time range")
  @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
  public ResponseEntity<List<EventDto>> getEventsByCalendarIdAndTimeRange(
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID calendarId,
      @Parameter(description = "Start time", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime start,
      @Parameter(description = "End time", required = true)
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime end) {
    log.debug(
        "GET request to retrieve events for calendar {} between {} and {}", calendarId, start, end);
    List<EventDto> events = eventService.getEventsByCalendarIdAndTimeRange(calendarId, start, end);
    log.info(
        "Retrieved {} events for calendar {} between {} and {}",
        events.size(),
        calendarId,
        start,
        end);
    return ResponseEntity.ok(events);
  }

  /**
   * Creates a new event.
   *
   * @param eventDto The event data
   * @return The created event
   */
  @PostMapping
  @Operation(summary = "Create event", description = "Creates a new event")
  @ApiResponse(responseCode = "201", description = "Event created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Calendar not found",
      content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<EventDto> createEvent(
      @Parameter(description = "Event data", required = true) @Valid @RequestBody EventDto eventDto) {
    log.debug("POST request to create event with title: {}", eventDto.getTitle());
    EventDto createdEvent = eventService.createEvent(eventDto);
    log.info("Created event with id: {}", createdEvent.getId());
    return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
  }

  /**
   * Updates an existing event.
   *
   * @param id The event ID
   * @param eventDto The updated event data
   * @return The updated event
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update event", description = "Updates an existing event")
  @ApiResponse(responseCode = "200", description = "Event updated successfully")
  @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<EventDto> updateEvent(
      @Parameter(description = "Event ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Event data", required = true) @Valid @RequestBody EventDto eventDto) {
    log.debug("PUT request to update event with id: {}", id);
    EventDto updatedEvent = eventService.updateEvent(id, eventDto);
    log.info("Updated event with id: {}", id);
    return ResponseEntity.ok(updatedEvent);
  }

  /**
   * Deletes an event.
   *
   * @param id The event ID
   * @return No content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete event", description = "Deletes an event")
  @ApiResponse(responseCode = "204", description = "Event deleted successfully")
  @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<Void> deleteEvent(
      @Parameter(description = "Event ID", required = true) @PathVariable UUID id) {
    log.debug("DELETE request to delete event with id: {}", id);
    eventService.deleteEvent(id);
    log.info("Deleted event with id: {}", id);
    return ResponseEntity.noContent().build();
  }
}
