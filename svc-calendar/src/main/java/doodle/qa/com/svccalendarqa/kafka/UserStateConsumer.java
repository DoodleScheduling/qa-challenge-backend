package doodle.qa.com.svccalendarqa.kafka;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import doodle.qa.com.svccalendarqa.repository.UserCalendarRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for user state events. Listens to the user-state topic and processes user state
 * events. Only saves user_id and calendar_id to the user_calendar table.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserStateConsumer {

  private final UserCalendarRepository userCalendarRepository;

  /**
   * Processes user state events from the user-state topic.
   *
   * @param userState the user state event
   * @param acknowledgment the acknowledgment to manually acknowledge the message
   */
  @KafkaListener(
      topics = "${kafka.topics.user-state}",
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void processUserState(@Payload UserState userState, Acknowledgment acknowledgment) {
    try {
      log.info("Received user state event: {}", userState);

      UUID userId = UUID.fromString(userState.getId());
      EventType eventType = userState.getEventType();

      switch (eventType) {
        case CREATED:
        case UPDATED:
          handleUserCreatedOrUpdated(userState);
          break;
        case DELETED:
          handleUserDeleted(userId);
          break;
        case CALENDAR_ADDED:
        case CALENDAR_REMOVED:
          handleCalendarChange(userState);
          break;
        default:
          log.warn("Unknown event type: {}", eventType);
      }

      // Acknowledge the message
      acknowledgment.acknowledge();
      log.info("Successfully processed user state event: {}", userState);
    } catch (Exception e) {
      log.error("Error processing user state event: {}", userState, e);
      // Don't acknowledge the message to let the error handler deal with it
      // The DefaultErrorHandler configured in KafkaConfig will handle retries and DLT
      throw e;
    }
  }

  /**
   * Handles user created or updated events. Only saves user_id and calendar_id to the user_calendar
   * table.
   *
   * @param userState the user state event
   */
  private void handleUserCreatedOrUpdated(UserState userState) {
    UUID userId = UUID.fromString(userState.getId());

    // Process calendar IDs
    List<UUID> calendarIds;
    if (!userState.getCalendarIds().isEmpty()) {
      // Convert string calendar IDs to UUID
      calendarIds = userState.getCalendarIds().stream().map(UUID::fromString).toList();

      log.info(
          "Processing user created/updated event for user ID: {} with {} calendar(s)",
          userId,
          calendarIds.size());
    } else {
      log.info("No calendar IDs provided for user ID: {}", userId);
      return; // Skip processing if no calendar IDs are provided
    }

    // Create or update user calendars
    for (UUID calendarId : calendarIds) {
      UserCalendar userCalendar =
          userCalendarRepository
              .findByCalendarId(calendarId)
              .orElse(UserCalendar.builder().calendarId(calendarId).userId(userId).build());

      userCalendarRepository.save(userCalendar);
      log.info("User calendar created/updated: {}", userCalendar);
    }

    // Remove any calendars that are no longer associated with the user
    List<UserCalendar> existingCalendars = userCalendarRepository.findByUserId(userId);
    for (UserCalendar existingCalendar : existingCalendars) {
      if (!calendarIds.contains(existingCalendar.getCalendarId())) {
        userCalendarRepository.delete(existingCalendar);
        log.info("User calendar removed: {}", existingCalendar);
      }
    }
  }

  /**
   * Handles user deleted events. Only deletes entries from the user_calendar table.
   *
   * @param userId the user ID
   */
  private void handleUserDeleted(UUID userId) {
    // Delete all user calendars associated with this user
    List<UserCalendar> userCalendars = userCalendarRepository.findByUserId(userId);
    if (!userCalendars.isEmpty()) {
      userCalendarRepository.deleteAll(userCalendars);
      log.info("Deleted {} user calendars for user ID: {}", userCalendars.size(), userId);
    } else {
      log.info("No user calendars found for user ID: {}", userId);
    }
  }

  /**
   * Handles calendar added or removed events. Only updates entries in the user_calendar table.
   *
   * @param userState the user state event
   */
  private void handleCalendarChange(UserState userState) {
    UUID userId = UUID.fromString(userState.getId());
    EventType eventType = userState.getEventType();

    // Process calendar IDs
    List<UUID> calendarIds = new ArrayList<>();
    if (!userState.getCalendarIds().isEmpty()) {
      // Convert string calendar IDs to UUID
      calendarIds =
          userState.getCalendarIds().stream().map(UUID::fromString).toList();
    }

    log.info(
        "Processing calendar change event for user ID: {} with event type: {} and {} calendar(s)",
        userId,
        eventType,
        calendarIds.size());

    if (eventType == EventType.CALENDAR_ADDED) {
      // Skip processing if there are no calendar IDs to add
      if (calendarIds.isEmpty()) {
        log.info("No calendar IDs provided for CALENDAR_ADDED event for user ID: {}", userId);
        return;
      }

      // Create or update user calendars
      for (UUID calendarId : calendarIds) {
        UserCalendar userCalendar =
            userCalendarRepository
                .findByCalendarId(calendarId)
                .orElse(UserCalendar.builder().calendarId(calendarId).userId(userId).build());

        userCalendarRepository.save(userCalendar);
        log.info("User calendar added: {}", userCalendar);
      }
    } else if (eventType == EventType.CALENDAR_REMOVED) {
      // Remove calendars that are no longer associated with the user
      List<UserCalendar> existingCalendars = userCalendarRepository.findByUserId(userId);
      for (UserCalendar existingCalendar : existingCalendars) {
        if (!calendarIds.contains(existingCalendar.getCalendarId())) {
          userCalendarRepository.delete(existingCalendar);
          log.info("User calendar removed: {}", existingCalendar);
        }
      }
    }
  }
}
