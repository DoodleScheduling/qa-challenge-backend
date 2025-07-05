package doodle.qa.com.svcuserqa.unit.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcuserqa.controller.UserController;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.exception.UserNotFoundException;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for the UserController. These tests verify the REST API endpoints using MockMvc. */
@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private UserService userService;

  @Test
  @DisplayName("Should return all users when getting all users without explicit pagination")
  void getAllUsers_WithDefaultPagination_ShouldReturnPagedUsers() throws Exception {
    // Given
    List<UserDto> users = TestDataFactory.createUserDtoList(3);
    Page<UserDto> userPage = new PageImpl<>(users, PageRequest.of(0, 20), users.size());

    // Mock the service to return the page when called with any Pageable
    when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);

    // When/Then
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(3)))
        .andExpect(jsonPath("$.users[0].name", is(users.get(0).getName())))
        .andExpect(jsonPath("$.users[1].name", is(users.get(1).getName())))
        .andExpect(jsonPath("$.users[2].name", is(users.get(2).getName())))
        .andExpect(jsonPath("$.totalPages", is(1)))
        .andExpect(jsonPath("$.currentPage", is(0)));

    verify(userService).getAllUsers(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return paginated users when getting users with pagination parameters")
  void getAllUsers_WithPagination_ShouldReturnPagedUsers() throws Exception {
    // Given
    List<UserDto> users = TestDataFactory.createUserDtoList(5);
    int pageSize = 2;
    int pageNumber = 1; // Second page (0-indexed)

    // Create a Page object with the users
    Page<UserDto> userPage =
        new PageImpl<>(
            users.subList(
                pageNumber * pageSize, Math.min((pageNumber + 1) * pageSize, users.size())),
            PageRequest.of(pageNumber, pageSize),
            users.size());

    // Mock the service to return the page when called with the pageable parameter
    when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);

    // When/Then
    mockMvc
        .perform(
            get("/api/users")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(pageSize)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is((int) Math.ceil((double) users.size() / pageSize))));

    verify(userService).getAllUsers(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page when getting users with page number out of bounds")
  void getAllUsers_WithPageOutOfBounds_ShouldReturnEmptyPage() throws Exception {
    // Given
    List<UserDto> users = TestDataFactory.createUserDtoList(5);
    int pageSize = 10;
    int pageNumber = 10; // Page that doesn't exist

    // Create an empty Page object
    Page<UserDto> emptyPage =
        new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), users.size());

    // Mock the service to return the empty page
    when(userService.getAllUsers(any(Pageable.class))).thenReturn(emptyPage);

    // When/Then
    mockMvc
        .perform(
            get("/api/users")
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.users", hasSize(0)))
        .andExpect(jsonPath("$.currentPage", is(pageNumber)))
        .andExpect(jsonPath("$.totalPages", is(1)));

    verify(userService).getAllUsers(any(Pageable.class));
  }

  @Test
  @DisplayName("Should return user when getting user by ID that exists")
  void getUserById_WhenUserExists_ShouldReturnUser() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserDto user = TestDataFactory.createUserDto(userId, "Test User", "test@example.com", null);
    when(userService.getUserById(userId)).thenReturn(user);

    // When/Then
    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.name", is("Test User")))
        .andExpect(jsonPath("$.email", is("test@example.com")));

    verify(userService).getUserById(userId);
  }

  @Test
  @DisplayName("Should return 404 when getting user by ID that doesn't exist")
  void getUserById_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    when(userService.getUserById(userId)).thenThrow(new UserNotFoundException(userId));

    // When/Then
    mockMvc.perform(get("/api/users/{id}", userId)).andExpect(status().isNotFound());

    verify(userService).getUserById(userId);
  }

  @Test
  @DisplayName("Should create user when creating user with valid data")
  void createUser_WithValidData_ShouldCreateUser() throws Exception {
    // Given
    UserDto userToCreate = TestDataFactory.createUserDto("New User", "new@example.com");
    UserDto createdUser =
        TestDataFactory.createUserDto(UUID.randomUUID(), "New User", "new@example.com", null);
    when(userService.createUser(any(UserDto.class))).thenReturn(createdUser);

    // When/Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToCreate)))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(createdUser.getId().toString())))
        .andExpect(jsonPath("$.name", is("New User")))
        .andExpect(jsonPath("$.email", is("new@example.com")));

    verify(userService).createUser(any(UserDto.class));
  }

  @Test
  @DisplayName("Should return 400 when creating user with invalid data")
  void createUser_WithInvalidData_ShouldReturn400() throws Exception {
    // Given
    UserDto invalidUser =
        UserDto.builder().email("invalid-email").build(); // Missing name and invalid email

    // When/Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).createUser(any(UserDto.class));
  }

  @Test
  @DisplayName("Should update user when updating user that exists with valid data")
  void updateUser_WhenUserExistsWithValidData_ShouldUpdateUser() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserDto userToUpdate =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", null);
    when(userService.updateUser(eq(userId), any(UserDto.class))).thenReturn(userToUpdate);

    // When/Then
    mockMvc
        .perform(
            put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToUpdate)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.name", is("Updated User")))
        .andExpect(jsonPath("$.email", is("updated@example.com")));

    verify(userService).updateUser(eq(userId), any(UserDto.class));
  }

  @Test
  @DisplayName("Should return 404 when updating user that doesn't exist")
  void updateUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UserDto userToUpdate =
        TestDataFactory.createUserDto(userId, "Updated User", "updated@example.com", null);
    when(userService.updateUser(eq(userId), any(UserDto.class)))
        .thenThrow(new UserNotFoundException("User not found with id: " + userId));

    // When/Then
    mockMvc
        .perform(
            put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToUpdate)))
        .andExpect(status().isNotFound());

    verify(userService).updateUser(eq(userId), any(UserDto.class));
  }

  @Test
  @DisplayName("Should delete user when deleting user that exists")
  void deleteUser_WhenUserExists_ShouldDeleteUser() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    doNothing().when(userService).deleteUser(userId);

    // When/Then
    mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());

    verify(userService).deleteUser(userId);
  }

  @Test
  @DisplayName("Should return 404 when deleting user that doesn't exist")
  void deleteUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    doThrow(new UserNotFoundException("User not found with id: " + userId))
        .when(userService)
        .deleteUser(userId);

    // When/Then
    mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNotFound());

    verify(userService).deleteUser(userId);
  }

  @Test
  @DisplayName("Should add calendar to user when user exists")
  void addCalendarToUser_WhenUserExists_ShouldAddCalendar() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserDto updatedUser =
        TestDataFactory.createUserDto(userId, "User", "user@example.com", List.of(calendarId));
    when(userService.addCalendarToUser(userId, calendarId)).thenReturn(updatedUser);

    // When/Then
    mockMvc
        .perform(post("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.calendarIds", hasSize(1)))
        .andExpect(jsonPath("$.calendarIds[0]", is(calendarId.toString())));

    verify(userService).addCalendarToUser(userId, calendarId);
  }

  @Test
  @DisplayName("Should return 404 when adding calendar to user that doesn't exist")
  void addCalendarToUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    when(userService.addCalendarToUser(userId, calendarId))
        .thenThrow(new UserNotFoundException("User not found with id: " + userId));

    // When/Then
    mockMvc
        .perform(post("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isNotFound());

    verify(userService).addCalendarToUser(userId, calendarId);
  }

  @Test
  @DisplayName("Should remove calendar from user when user exists")
  void removeCalendarFromUser_WhenUserExists_ShouldRemoveCalendar() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    UserDto updatedUser = TestDataFactory.createUserDto(userId, "User", "user@example.com", null);
    when(userService.removeCalendarFromUser(userId, calendarId)).thenReturn(updatedUser);

    // When/Then
    mockMvc
        .perform(delete("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(userId.toString())))
        .andExpect(jsonPath("$.calendarIds", hasSize(0)));

    verify(userService).removeCalendarFromUser(userId, calendarId);
  }

  @Test
  @DisplayName("Should return 404 when removing calendar from user that doesn't exist")
  void removeCalendarFromUser_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    when(userService.removeCalendarFromUser(userId, calendarId))
        .thenThrow(new UserNotFoundException("User not found with id: " + userId));

    // When/Then
    mockMvc
        .perform(delete("/api/users/{userId}/calendars/{calendarId}", userId, calendarId))
        .andExpect(status().isNotFound());

    verify(userService).removeCalendarFromUser(userId, calendarId);
  }
}
