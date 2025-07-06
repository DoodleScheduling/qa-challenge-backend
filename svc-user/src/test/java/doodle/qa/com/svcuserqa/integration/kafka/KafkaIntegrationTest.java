package doodle.qa.com.svcuserqa.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Kafka messaging. These tests verify that Kafka messages are correctly
 * produced and consumed when user operations are performed.
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
class KafkaIntegrationTest {

  @Autowired private UserService userService;

  @Autowired private ConsumerFactory<String, Object> consumerFactory;

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  private Consumer<String, Object> consumer;

  @BeforeEach
  void setUp() {
    consumer = consumerFactory.createConsumer("kafka-integration-test", "client-1");
    consumer.subscribe(Collections.singletonList(userStateTopic));
    // Clear any existing messages
    consumer.poll(Duration.ofMillis(100));
  }

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  @DisplayName("Should produce Kafka message when creating a user")
  void testCreateUserProducesKafkaMessage() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Kafka Test User", "kafka-test@example.com");

    // When
    UserDto createdUser = userService.createUser(userDto);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              assertThat(records.count()).isGreaterThan(0);

              boolean messageFound = false;
              for (ConsumerRecord<String, Object> record : records) {
                if (record.key().equals(createdUser.getId().toString())) {
                  UserState userState = (UserState) record.value();
                  assertThat(userState.getId()).isEqualTo(createdUser.getId().toString());
                  assertThat(userState.getName()).isEqualTo("Kafka Test User");
                  assertThat(userState.getEmail()).isEqualTo("kafka-test@example.com");
                  assertThat(userState.getEventType()).isEqualTo(EventType.CREATED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });
  }

  @Test
  @DisplayName("Should produce Kafka message when updating a user")
  void testUpdateUserProducesKafkaMessage() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Original User", "original@example.com");
    UserDto createdUser = userService.createUser(userDto);

    // Clear messages from creation
    consumer.poll(Duration.ofMillis(100));

    UserDto updateDto =
        TestDataFactory.createUserDto(
            createdUser.getId(), "Updated User", "updated@example.com", null);

    // When
    UserDto updatedUser = userService.updateUser(createdUser.getId(), updateDto);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              assertThat(records.count()).isGreaterThan(0);

              boolean messageFound = false;
              for (ConsumerRecord<String, Object> record : records) {
                if (record.key().equals(updatedUser.getId().toString())) {
                  UserState userState = (UserState) record.value();
                  assertThat(userState.getId()).isEqualTo(updatedUser.getId().toString());
                  assertThat(userState.getName()).isEqualTo("Updated User");
                  assertThat(userState.getEmail()).isEqualTo("updated@example.com");
                  assertThat(userState.getEventType()).isEqualTo(EventType.UPDATED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });
  }

  @Test
  @DisplayName("Should produce Kafka message when deleting a user")
  void testDeleteUserProducesKafkaMessage() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("User To Delete", "delete@example.com");
    UserDto createdUser = userService.createUser(userDto);

    // Clear messages from creation
    consumer.poll(Duration.ofMillis(100));

    UUID userId = createdUser.getId();

    // When
    userService.deleteUser(userId);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              assertThat(records.count()).isGreaterThan(0);

              boolean messageFound = false;
              for (ConsumerRecord<String, Object> record : records) {
                if (record.key().equals(userId.toString())) {
                  UserState userState = (UserState) record.value();
                  assertThat(userState.getId()).isEqualTo(userId.toString());
                  assertThat(userState.getEventType()).isEqualTo(EventType.DELETED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });
  }

  @Test
  @DisplayName("Should produce Kafka message when adding a calendar to a user")
  void testAddCalendarToUserProducesKafkaMessage() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Calendar User", "calendar@example.com");
    UserDto createdUser = userService.createUser(userDto);

    // Clear messages from creation
    consumer.poll(Duration.ofMillis(100));

    UUID userId = createdUser.getId();
    UUID calendarId = UUID.randomUUID();

    // When
    UserDto updatedUser = userService.addCalendarToUser(userId, calendarId);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              assertThat(records.count()).isGreaterThan(0);

              boolean messageFound = false;
              for (ConsumerRecord<String, Object> record : records) {
                if (record.key().equals(userId.toString())) {
                  UserState userState = (UserState) record.value();
                  assertThat(userState.getId()).isEqualTo(userId.toString());
                  assertThat(userState.getCalendarIds()).contains(calendarId.toString());
                  assertThat(userState.getEventType()).isEqualTo(EventType.CALENDAR_ADDED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });
  }

  @Test
  @DisplayName("Should produce Kafka message when removing a calendar from a user")
  void testRemoveCalendarFromUserProducesKafkaMessage() {
    // Given
    UUID calendarId = UUID.randomUUID();
    UserDto userDto =
        TestDataFactory.createUserDto(
            "Calendar Remove User",
            "calendar-remove@example.com",
            Collections.singletonList(calendarId));
    UserDto createdUser = userService.createUser(userDto);

    // Clear messages from creation
    consumer.poll(Duration.ofMillis(100));

    UUID userId = createdUser.getId();

    // When
    UserDto updatedUser = userService.removeCalendarFromUser(userId, calendarId);

    // Then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
              assertThat(records.count()).isGreaterThan(0);

              boolean messageFound = false;
              for (ConsumerRecord<String, Object> record : records) {
                if (record.key().equals(userId.toString())) {
                  UserState userState = (UserState) record.value();
                  assertThat(userState.getId()).isEqualTo(userId.toString());
                  assertThat(userState.getCalendarIds()).doesNotContain(calendarId.toString());
                  assertThat(userState.getEventType()).isEqualTo(EventType.CALENDAR_REMOVED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });
  }
}
