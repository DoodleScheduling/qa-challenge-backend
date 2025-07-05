package doodle.qa.com.svcuserqa.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.exception.ConcurrentModificationException;
import doodle.qa.com.svcuserqa.kafka.UserStateProducer;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Kafka transaction functionality. These tests verify that Kafka messages are
 * correctly handled within transactions.
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
class KafkaTransactionTest {

  @Autowired private UserService userService;

  @SpyBean private UserRepository userRepository;

  @SpyBean private UserStateProducer userStateProducer;

  @Autowired private ConsumerFactory<String, Object> consumerFactory;

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  private Consumer<String, Object> consumer;

  @BeforeEach
  void setUp() {
    consumer = consumerFactory.createConsumer("kafka-transaction-test", "client-1");
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
  @DisplayName("Should not produce Kafka message when database operation fails")
  void testTransactionRollbackOnDatabaseFailure() {
    // Given
    UserDto userDto =
        TestDataFactory.createUserDto("Transaction Test User", "transaction-test@example.com");

    // Mock repository to throw exception after the first save
    Mockito.doThrow(new OptimisticLockingFailureException("Simulated database failure"))
        .when(userRepository)
        .save(Mockito.any(User.class));

    // When/Then
    assertThatThrownBy(() -> userService.createUser(userDto))
        .isInstanceOf(ConcurrentModificationException.class);

    // Verify that no Kafka message was produced
    ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(1000));
    assertThat(records.count()).isEqualTo(0);

    // Verify that sendUserState was never called
    Mockito.verify(userStateProducer, Mockito.never())
        .sendUserState(Mockito.any(), Mockito.eq(EventType.CREATED));
  }

  @Test
  @DisplayName("Should produce Kafka message when database operation succeeds")
  void testTransactionCommitOnDatabaseSuccess() {
    // Given
    UserDto userDto =
        TestDataFactory.createUserDto(
            "Transaction Success User", "transaction-success@example.com");

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
                  assertThat(userState.getName()).isEqualTo("Transaction Success User");
                  assertThat(userState.getEmail()).isEqualTo("transaction-success@example.com");
                  assertThat(userState.getEventType()).isEqualTo(EventType.CREATED);
                  messageFound = true;
                  break;
                }
              }
              assertThat(messageFound).isTrue();
            });

    // Verify that sendUserState was called
    Mockito.verify(userStateProducer).sendUserState(Mockito.any(), Mockito.eq(EventType.CREATED));
  }
}
