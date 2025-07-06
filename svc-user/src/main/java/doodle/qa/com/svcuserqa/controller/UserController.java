package doodle.qa.com.svcuserqa.controller;

import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User API", description = "API for user management")
public class UserController {

  private final UserService userService;

  /**
   * Retrieves all users with pagination.
   *
   * @param page Page number (zero-based, optional)
   * @param size Number of users per page (optional)
   * @return Simplified response with users and minimal pagination information
   */
  @GetMapping
  @Operation(
      summary = "Get all users",
      description =
          "Retrieves a list of all users with pagination. Use page and size parameters for pagination.")
  @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
  public ResponseEntity<Map<String, Object>> getAllUsers(
      @Parameter(description = "Page number (0-indexed)")
          @RequestParam(required = false, defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20")
          Integer size) {

    Pageable pageable = PageRequest.of(page, size);
    log.debug("GET request to retrieve all users with pagination: page={}, size={}", page, size);

    Page<UserDto> usersPage = userService.getAllUsers(pageable);

    // Create a simplified response with minimal pagination information
    Map<String, Object> response = new HashMap<>();
    response.put("users", usersPage.getContent());
    response.put("totalPages", usersPage.getTotalPages());
    response.put("currentPage", usersPage.getNumber());

    log.info(
        "Retrieved {} users (page {} of {})",
        usersPage.getNumberOfElements(),
        usersPage.getNumber() + 1,
        usersPage.getTotalPages());

    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a user by ID.
   *
   * @param id The user ID
   * @return The user with the specified ID
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
  @ApiResponse(responseCode = "200", description = "User retrieved successfully")
  @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  public ResponseEntity<UserDto> getUserById(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id) {
    log.debug("GET request to retrieve user with id: {}", id);
    UserDto user = userService.getUserById(id);
    log.info("Retrieved user with id: {}", id);
    return ResponseEntity.ok(user);
  }

  /**
   * Creates a new user.
   *
   * @param userDto The user data
   * @return The created user
   */
  @PostMapping
  @Operation(summary = "Create user", description = "Creates a new user")
  @ApiResponse(responseCode = "201", description = "User created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<UserDto> createUser(
      @Parameter(description = "User data", required = true) @Valid @RequestBody UserDto userDto) {
    log.debug("POST request to create user with email: {}", userDto.getEmail());
    UserDto createdUser = userService.createUser(userDto);
    log.info("Created user with id: {}", createdUser.getId());
    return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
  }

  /**
   * Updates an existing user.
   *
   * @param id The user ID
   * @param userDto The updated user data
   * @return The updated user
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update user", description = "Updates an existing user")
  @ApiResponse(responseCode = "200", description = "User updated successfully")
  @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<UserDto> updateUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id,
      @Parameter(description = "User data", required = true) @Valid @RequestBody UserDto userDto) {
    log.debug("PUT request to update user with id: {}", id);
    UserDto updatedUser = userService.updateUser(id, userDto);
    log.info("Updated user with id: {}", id);
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Deletes a user.
   *
   * @param id The user ID
   * @return No content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete user", description = "Deletes a user")
  @ApiResponse(responseCode = "204", description = "User deleted successfully")
  @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID id) {
    log.debug("DELETE request to delete user with id: {}", id);
    userService.deleteUser(id);
    log.info("Deleted user with id: {}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Adds a calendar to a user.
   *
   * @param userId The user ID
   * @param calendarId The calendar ID to add
   * @return The updated user
   */
  @PostMapping("/{userId}/calendars/{calendarId}")
  @Operation(
      summary = "Add calendar to user",
      description = "Adds a calendar to a user's calendar list")
  @ApiResponse(responseCode = "200", description = "Calendar added successfully")
  @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - calendar already added or concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<UserDto> addCalendarToUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID calendarId) {
    log.debug("POST request to add calendar {} to user {}", calendarId, userId);
    UserDto updatedUser = userService.addCalendarToUser(userId, calendarId);
    log.info("Added calendar {} to user {}", calendarId, userId);
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Removes a calendar from a user.
   *
   * @param userId The user ID
   * @param calendarId The calendar ID to remove
   * @return The updated user
   */
  @DeleteMapping("/{userId}/calendars/{calendarId}")
  @Operation(
      summary = "Remove calendar from user",
      description = "Removes a calendar from a user's calendar list")
  @ApiResponse(responseCode = "200", description = "Calendar removed successfully")
  @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict - calendar already added or concurrent modification",
      content = @Content)
  @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
  public ResponseEntity<UserDto> removeCalendarFromUser(
      @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
      @Parameter(description = "Calendar ID", required = true) @PathVariable UUID calendarId) {
    log.debug("DELETE request to remove calendar {} from user {}", calendarId, userId);
    UserDto updatedUser = userService.removeCalendarFromUser(userId, calendarId);
    log.info("Removed calendar {} from user {}", calendarId, userId);
    return ResponseEntity.ok(updatedUser);
  }
}
