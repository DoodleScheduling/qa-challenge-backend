package doodle.qa.com.svccalendarqa.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svccalendarqa.entity.User;
import doodle.qa.com.svccalendarqa.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Kafka user state consumer. These tests verify that Kafka messages are
 * correctly consumed and processed to update local user data.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"user-state-test", "user-state-test.DLT"},
    brokerProperties = {
      "transaction.state.log.replication.factor=1",
      "transaction.state.log.min.isr=1"
    })
class UserStateConsumerIntegrationTest {

  @Autowired private UserRepository userRepository;

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create user when receiving CREATED event")
  void testCreateUserFromKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();
    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Test User")
            .setEmail("test@example.com")
            .setCalendarIds(new ArrayList<>())
            .setEventType(EventType.CREATED)
            .setTimestamp(Instant.now().toEpochMilli())
            .build();

    // When
    kafkaTemplate.send(userStateTopic, userId.toString(), userState);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              User user = userRepository.findById(userId).orElse(null);
              assertThat(user).isNotNull();
              assertThat(user.getName()).isEqualTo("Test User");
              assertThat(user.getEmail()).isEqualTo("test@example.com");
            });
  }

  @Test
  @DisplayName("Should update user when receiving UPDATED event")
  void testUpdateUserFromKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();

    // Create initial user
    User initialUser =
        User.builder().id(userId).name("Initial Name").email("initial@example.com").build();
    userRepository.save(initialUser);

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("Updated Name")
            .setEmail("updated@example.com")
            .setCalendarIds(new ArrayList<>())
            .setEventType(EventType.UPDATED)
            .setTimestamp(Instant.now().toEpochMilli())
            .build();

    // When
    kafkaTemplate.send(userStateTopic, userId.toString(), userState);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              User user = userRepository.findById(userId).orElse(null);
              assertThat(user).isNotNull();
              assertThat(user.getName()).isEqualTo("Updated Name");
              assertThat(user.getEmail()).isEqualTo("updated@example.com");
            });
  }

  @Test
  @DisplayName("Should delete user when receiving DELETED event")
  void testDeleteUserFromKafkaMessage() {
    // Given
    UUID userId = UUID.randomUUID();

    // Create initial user
    User initialUser =
        User.builder().id(userId).name("User To Delete").email("delete@example.com").build();
    userRepository.save(initialUser);

    UserState userState =
        UserState.newBuilder()
            .setId(userId.toString())
            .setName("User To Delete")
            .setEmail("delete@example.com")
            .setCalendarIds(new ArrayList<>())
            .setEventType(EventType.DELETED)
            .setTimestamp(Instant.now().toEpochMilli())
            .build();

    // When
    kafkaTemplate.send(userStateTopic, userId.toString(), userState);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              boolean userExists = userRepository.existsById(userId);
              assertThat(userExists).isFalse();
            });
  }
}
