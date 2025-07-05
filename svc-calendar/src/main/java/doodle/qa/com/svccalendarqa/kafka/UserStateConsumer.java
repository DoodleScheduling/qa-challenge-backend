package doodle.qa.com.svccalendarqa.kafka;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svccalendarqa.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for processing user state events. Listens to the user-state topic and updates
 * local user data accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStateConsumer {

  private final UserService userService;

  /**
   * Processes user state events from the user-state topic.
   *
   * @param userState The user state message
   * @param acknowledgment Manual acknowledgment for the message
   */
  @KafkaListener(
      topics = "${kafka.topics.user-state}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  @Transactional
  @Retryable(
      value = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  public void handleUserState(@Payload UserState userState, Acknowledgment acknowledgment) {

    log.info("Received user state message: eventType={}", userState.getEventType());

    try {
      UUID userId = UUID.fromString(userState.getId());
      EventType eventType = userState.getEventType();

      switch (eventType) {
        case CREATED:
        case UPDATED:
        case CALENDAR_ADDED:
        case CALENDAR_REMOVED:
          log.debug("Processing user create/update for user: {}", userId);
          userService.createOrUpdateUser(userId, userState.getName(), userState.getEmail());
          break;

        case DELETED:
          log.debug("Processing user deletion for user: {}", userId);
          userService.deleteUser(userId);
          break;

        default:
          log.warn("Unknown event type: {} for user: {}", eventType, userId);
          break;
      }

      // Manual acknowledgment after successful processing
      acknowledgment.acknowledge();
      log.debug("Successfully processed user state message for user: {}", userId);

    } catch (Exception e) {
      log.error("Error processing user state message: eventType={}", userState.getEventType(), e);

      // Re-throw to trigger retry mechanism
      throw e;
    }
  }
}
