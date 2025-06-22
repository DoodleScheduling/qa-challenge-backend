package doodle.qa.com.svcuserqa.integration.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the UserController. These tests verify the REST API endpoints with the
 * actual service implementation, ensuring that the controller correctly interacts with the service
 * layer and returns appropriate responses for various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-state-test", "user-state-test.DLT"})
class UserControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserService userService;

  @Test
  @DisplayName("Should create a user and then retrieve it by ID and in the list of all users")
  void testCreateAndRetrieveUser() throws Exception {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Test User", "test@example.com");
    String userJson = objectMapper.writeValueAsString(userDto);

    // When/Then - Create user
    String responseJson =
        mockMvc
            .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(userJson))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Test User")))
            .andExpect(jsonPath("$.email", is("test@example.com")))
            // Version field is marked with @JsonIgnore, so it's not included in the response
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract ID from response
    UserDto createdUser = objectMapper.readValue(responseJson, UserDto.class);
    UUID userId = createdUser.getId();

    // When/Then - Get user by ID
    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.name", is("Test User")))
        .andExpect(jsonPath("$.email", is("test@example.com")));

    // When/Then - Get all users with pagination
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(1)))
        .andExpect(jsonPath("$.users[0].id", is(userId.toString())))
        .andExpect(jsonPath("$.totalPages").exists())
        .andExpect(jsonPath("$.currentPage").exists());
  }

  @Test
  @DisplayName("Should update a user's information")
  void testUpdateUser() throws Exception {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Original User", "original@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();

    UserDto updateDto =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", null);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    // When/Then
    mockMvc
        .perform(
            put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.name", is("Updated User")))
        .andExpect(jsonPath("$.email", is("updated@example.com")));
  }

  @Test
  @DisplayName("Should delete a user")
  void testDeleteUser() throws Exception {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("User To Delete", "delete@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();

    // When/Then - Delete user
    mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());

    // Verify user is deleted
    mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should add and remove a calendar from a user")
  void testAddAndRemoveCalendarFromUser() throws Exception {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar User", "calendar@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();
    UUID calendarId = UUID.randomUUID();

    // When/Then - Add calendar
    mockMvc
        .perform(post("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.calendarIds", hasSize(1)))
        .andExpect(jsonPath("$.calendarIds[0]", is(calendarId.toString())));

    // When/Then - Remove calendar
    mockMvc
        .perform(delete("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.calendarIds", hasSize(0)));
  }

  @Test
  @DisplayName("Should return 404 when removing a calendar that doesn't exist")
  void testRemoveNonExistentCalendar() throws Exception {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar User", "calendar-test@example.com");
    UserDto createdUser = userService.createUser(userDto);
    UUID userId = createdUser.getId();
    UUID nonExistentCalendarId = UUID.randomUUID();

    // When/Then - Try to remove a non-existent calendar
    mockMvc
        .perform(
            delete("/api/users/{userId}/calendars/{calendarId}", userId, nonExistentCalendarId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is(404)))
        .andExpect(jsonPath("$.message", containsString("Calendar not found")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("Should return 404 when user is not found")
  void testUserNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    // When/Then - Get non-existent user
    mockMvc.perform(get("/api/users/{id}", nonExistentId)).andExpect(status().isNotFound());

    // When/Then - Update non-existent user
    UserDto updateDto =
        TestDataFactory.createUserDto(nonExistentId, "Updated User", "updated@example.com", null);
    String updateJson = objectMapper.writeValueAsString(updateDto);

    mockMvc
        .perform(
            put("/api/users/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
        .andExpect(status().isNotFound());

    // When/Then - Delete non-existent user
    mockMvc.perform(delete("/api/users/{id}", nonExistentId)).andExpect(status().isNotFound());

    // When/Then - Add calendar to non-existent user
    mockMvc
        .perform(
            post("/api/users/{userId}/calendars/{calendarId}", nonExistentId, UUID.randomUUID()))
        .andExpect(status().isNotFound());

    // When/Then - Remove calendar from non-existent user
    mockMvc
        .perform(
            delete("/api/users/{userId}/calendars/{calendarId}", nonExistentId, UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when creating a user with invalid data")
  void testCreateUserWithInvalidData() throws Exception {
    // Given
    UserDto invalidUser =
        UserDto.builder().email("invalid-email").build(); // Missing name and invalid email
    String invalidJson = objectMapper.writeValueAsString(invalidUser);

    // When/Then
    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should retrieve users with explicit pagination parameters")
  void testGetUsersWithPagination() throws Exception {
    // Given
    int totalUsers = 15;
    int pageSize = 5;
    int pageNumber = 1; // Second page (0-indexed)

    // Create multiple users
    for (int i = 0; i < totalUsers; i++) {
      userService.createUser(
          TestDataFactory.createUserDto("User " + i, "user" + i + "@example.com"));
    }

    // When/Then - Get users with pagination
    mockMvc
        .perform(
            get("/api/users")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is((int) Math.ceil((double) totalUsers / pageSize))));
  }

  @Test
  @DisplayName("Should handle empty page gracefully")
  void testGetUsersEmptyPage() throws Exception {
    // Given - No users in the database

    // When/Then - Get users with pagination
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(0)))
        .andExpect(jsonPath("$.totalPages", is(0)))
        .andExpect(jsonPath("$.currentPage", is(0)));
  }

  @Test
  @DisplayName("Should handle page number out of bounds gracefully")
  void testGetUsersPageOutOfBounds() throws Exception {
    // Given
    int totalUsers = 5;

    // Create some users
    for (int i = 0; i < totalUsers; i++) {
      userService.createUser(
          TestDataFactory.createUserDto("User " + i, "user" + i + "@example.com"));
    }

    // When/Then - Request a page that is out of bounds
    mockMvc
        .perform(
            get("/api/users")
                .param("page", "10") // Page that doesn't exist
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(0)))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(10)));
  }
}
