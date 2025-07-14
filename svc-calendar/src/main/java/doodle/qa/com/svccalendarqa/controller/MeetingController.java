package doodle.qa.com.svccalendarqa.controller;

import doodle.qa.com.svccalendarqa.dto.MeetingDto;
import doodle.qa.com.svccalendarqa.dto.TimeSlotDto;
import doodle.qa.com.svccalendarqa.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
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

/** Controller for managing meetings. */
@RestController
@RequestMapping("/meeting")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Meeting", description = "Meeting management API")
public class MeetingController {

  private final MeetingService meetingService;

  /**
   * Get meetings by user ID, calendar ID, and time range.
   *
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @param from the start time
   * @param to the end time
   * @param page the page number
   * @param size the page size
   * @return a page of meetings
   */
  @GetMapping
  @Operation(
      summary = "Get meetings",
      description = "Get meetings by user ID, calendar ID, and time range",
      responses = {
        @ApiResponse(responseCode = "200", description = "Meetings found"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "User or calendar not found",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getMeetings(
      @Parameter(description = "User ID") @RequestParam UUID userId,
      @Parameter(description = "Calendar ID") @RequestParam UUID calendarId,
      @Parameter(description = "Start time")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @Parameter(description = "End time")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

    log.info(
        "Getting meetings for user {} and calendar {} from {} to {}", userId, calendarId, from, to);

    Pageable pageable = PageRequest.of(page, size);
    Page<MeetingDto> meetings = meetingService.findMeetings(userId, calendarId, from, to, pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("meetings", meetings.getContent());
    response.put("totalPages", meetings.getTotalPages());
    response.put("currentPage", meetings.getNumber());

    return ResponseEntity.ok(response);
  }

  /**
   * Get available time slots by user ID, calendar ID, time range, and slot duration.
   *
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @param from the start time
   * @param to the end time
   * @param slotDuration the slot duration in minutes
   * @param page the page number
   * @param size the page size
   * @return a page of available time slots with pagination information
   */
  @GetMapping("/slots")
  @Operation(
      summary = "Get available time slots",
      description =
          "Get available time slots by user ID, calendar ID, time range, and slot duration",
      responses = {
        @ApiResponse(responseCode = "200", description = "Time slots found"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "User or calendar not found",
            content = @Content)
      })
  public ResponseEntity<Map<String, Object>> getAvailableTimeSlots(
      @Parameter(description = "User ID") @RequestParam UUID userId,
      @Parameter(description = "Calendar ID") @RequestParam UUID calendarId,
      @Parameter(description = "Start time")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @Parameter(description = "End time")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @Parameter(description = "Slot duration in minutes") @RequestParam int slotDuration,
      @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

    log.info(
        "Getting available time slots for user {} and calendar {} from {} to {} with slot duration {}",
        userId,
        calendarId,
        from,
        to,
        slotDuration);

    Pageable pageable = PageRequest.of(page, size);
    Page<TimeSlotDto> timeSlots =
        meetingService.findAvailableTimeSlots(userId, calendarId, from, to, slotDuration, pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("slots", timeSlots.getContent());
    response.put("totalPages", timeSlots.getTotalPages());
    response.put("currentPage", timeSlots.getNumber());

    return ResponseEntity.ok(response);
  }

  /**
   * Get a meeting by ID, user ID, and calendar ID.
   *
   * @param id the meeting ID
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @return the meeting
   */
  @GetMapping("/{id}")
  @Operation(
      summary = "Get a meeting",
      description = "Get a meeting by ID, user ID, and calendar ID",
      responses = {
        @ApiResponse(responseCode = "200", description = "Meeting found"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Meeting, user, or calendar not found",
            content = @Content)
      })
  public ResponseEntity<MeetingDto> getMeeting(
      @Parameter(description = "Meeting ID") @PathVariable UUID id,
      @Parameter(description = "User ID") @RequestParam UUID userId,
      @Parameter(description = "Calendar ID") @RequestParam UUID calendarId) {

    log.info("Getting meeting {} for user {} and calendar {}", id, userId, calendarId);

    MeetingDto meeting = meetingService.findMeeting(id, userId, calendarId);

    return ResponseEntity.ok(meeting);
  }

  /**
   * Create a meeting.
   *
   * @param meetingDto the meeting DTO
   * @param userId the user ID
   * @return the created meeting
   */
  @PostMapping
  @Operation(
      summary = "Create a meeting",
      description = "Create a meeting",
      responses = {
        @ApiResponse(responseCode = "201", description = "Meeting created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "User or calendar not found",
            content = @Content)
      })
  public ResponseEntity<MeetingDto> createMeeting(
      @Parameter(description = "Meeting DTO") @Valid @RequestBody MeetingDto meetingDto,
      @Parameter(description = "User ID") @RequestParam UUID userId) {

    log.info("Creating meeting for user {}", userId);

    MeetingDto meeting = meetingService.createMeeting(meetingDto, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(meeting);
  }

  /**
   * Update a meeting.
   *
   * @param id the meeting ID
   * @param meetingDto the meeting DTO
   * @param userId the user ID
   * @return the updated meeting
   */
  @PutMapping("/{id}")
  @Operation(
      summary = "Update a meeting",
      description = "Update a meeting",
      responses = {
        @ApiResponse(responseCode = "200", description = "Meeting updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Meeting, user, or calendar not found",
            content = @Content)
      })
  public ResponseEntity<MeetingDto> updateMeeting(
      @Parameter(description = "Meeting ID") @PathVariable UUID id,
      @Parameter(description = "Meeting DTO") @Valid @RequestBody MeetingDto meetingDto,
      @Parameter(description = "User ID") @RequestParam UUID userId) {

    log.info("Updating meeting {} for user {}", id, userId);

    MeetingDto meeting = meetingService.updateMeeting(id, meetingDto, userId);

    return ResponseEntity.ok(meeting);
  }

  /**
   * Delete a meeting.
   *
   * @param id the meeting ID
   * @param userId the user ID
   * @param calendarId the calendar ID
   * @return no content
   */
  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete a meeting",
      description = "Delete a meeting",
      responses = {
        @ApiResponse(responseCode = "204", description = "Meeting deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(
            responseCode = "404",
            description = "Meeting, user, or calendar not found",
            content = @Content)
      })
  public ResponseEntity<Void> deleteMeeting(
      @Parameter(description = "Meeting ID") @PathVariable UUID id,
      @Parameter(description = "User ID") @RequestParam UUID userId,
      @Parameter(description = "Calendar ID") @RequestParam UUID calendarId) {

    log.info("Deleting meeting {} for user {} and calendar {}", id, userId, calendarId);

    meetingService.deleteMeeting(id, userId, calendarId);

    return ResponseEntity.noContent().build();
  }
}
