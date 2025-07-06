package doodle.qa.com.svcuserqa.kafka;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svcuserqa.entity.User;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka producer for sending user state events. Extends the base KafkaProducer class to leverage
 * common Kafka messaging functionality.
 */
@Component
@Slf4j
@Validated
public class UserStateProducer extends KafkaProducer<String, Object> {

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  public UserStateProducer(KafkaTemplate<String, Object> kafkaTemplate) {
    super(kafkaTemplate);
  }

  /**
   * Sends a user state event to Kafka.
   *
   * @param user The user entity (must not be null)
   * @param eventType The type of event (CREATED, UPDATED, DELETED, etc.) (must not be null)
   */
  public void sendUserState(@NotNull User user, @NotNull EventType eventType) {
    UserState userState =
        UserState.newBuilder()
            .setId(user.getId().toString())
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setCalendarIds(
                user.getCalendarIds().stream().map(UUID::toString).collect(Collectors.toList()))
            .setEventType(eventType)
            .setTimestamp(Instant.now().toEpochMilli())
            .build();

    String key = user.getId().toString();

    log.info("Preparing user state for Kafka: {} with event type: {}", key, eventType);

    sendMessage(userStateTopic, key, userState);
  }
}
