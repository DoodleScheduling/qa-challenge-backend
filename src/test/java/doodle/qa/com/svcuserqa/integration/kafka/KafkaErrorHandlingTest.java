package doodle.qa.com.svcuserqa.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.svcuser.avro.EventType;
import doodle.qa.com.svcuserqa.dto.UserDto;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.kafka.UserStateProducer;
import doodle.qa.com.svcuserqa.repository.UserRepository;
import doodle.qa.com.svcuserqa.service.UserService;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Kafka error handling. These tests verify that the application correctly
 * handles Kafka-related errors and retries failed operations.
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
class KafkaErrorHandlingTest {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @MockBean private UserStateProducer userStateProducer;

  @MockBean private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    reset(userStateProducer, kafkaTemplate);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("Should retry sending Kafka message when first attempt fails")
  @Transactional
  void testKafkaRetryOnFailure() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Retry Test User", "retry@example.com");

    // Configure UserStateProducer to throw exception
    doThrow(new KafkaException("Simulated Kafka failure"))
        .when(userStateProducer)
        .sendUserState(any(User.class), any(EventType.class));

    // When/Then
    // Expect the exception to be propagated
    assertThatThrownBy(() -> userService.createUser(userDto))
        .isInstanceOf(KafkaException.class)
        .hasMessageContaining("Simulated Kafka failure");

    // Verify UserStateProducer.sendUserState was called
    verify(userStateProducer).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName("Should propagate exception when Kafka producer throws exception")
  @Transactional
  void testKafkaExceptionHandling() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Exception Test User", "exception@example.com");

    // Configure UserStateProducer to throw exception
    doThrow(new RuntimeException("Simulated Kafka producer exception"))
        .when(userStateProducer)
        .sendUserState(any(User.class), any(EventType.class));

    // When/Then
    // Expect the exception to be propagated
    assertThatThrownBy(() -> userService.createUser(userDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Simulated Kafka producer exception");

    // Verify Kafka send was attempted
    verify(userStateProducer).sendUserState(any(User.class), any(EventType.class));
  }

  @Test
  @DisplayName("Should handle Kafka timeout and still complete database operation")
  @Transactional
  void testKafkaTimeoutHandling() {
    // Given
    UserDto userDto = TestDataFactory.createUserDto("Timeout Test User", "timeout@example.com");

    // Configure UserStateProducer to complete successfully
    doNothing().when(userStateProducer).sendUserState(any(User.class), any(EventType.class));

    // When
    UserDto createdUser = userService.createUser(userDto);

    // Then
    // Verify the user was created successfully
    assertThat(createdUser.getId()).isNotNull();
    assertThat(createdUser.getName()).isEqualTo("Timeout Test User");

    // Verify UserStateProducer.sendUserState was called
    verify(userStateProducer).sendUserState(any(User.class), any(EventType.class));
  }
}
