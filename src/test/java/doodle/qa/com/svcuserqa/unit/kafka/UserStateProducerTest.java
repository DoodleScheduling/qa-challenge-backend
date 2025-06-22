package doodle.qa.com.svcuserqa.unit.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.svcuser.avro.EventType;
import com.example.svcuser.avro.UserState;
import doodle.qa.com.svcuserqa.entity.User;
import doodle.qa.com.svcuserqa.kafka.UserStateProducer;
import doodle.qa.com.svcuserqa.util.TestDataFactory;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the UserStateProducer. These tests verify the Kafka message publishing
 * functionality.
 */
@ExtendWith(MockitoExtension.class)
class UserStateProducerTest {

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock private SendResult<String, Object> sendResult;

  private UserStateProducer userStateProducer;

  private final String userStateTopic = "user-state-test";

  @BeforeEach
  void setUp() {
    userStateProducer = new UserStateProducer(kafkaTemplate);
    ReflectionTestUtils.setField(userStateProducer, "userStateTopic", userStateTopic);
  }

  @Test
  @DisplayName("Should send user state message to Kafka when sending user state")
  void sendUserState_ShouldSendMessageToKafka() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    User user =
        TestDataFactory.createUser(userId, "Test User", "test@example.com", List.of(calendarId));
    EventType eventType = EventType.CREATED;

    CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
    future.complete(sendResult);
    when(kafkaTemplate.send(anyString(), anyString(), any(UserState.class))).thenReturn(future);

    // When
    userStateProducer.sendUserState(user, eventType);

    // Then
    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<UserState> valueCaptor = ArgumentCaptor.forClass(UserState.class);

    verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

    assertThat(topicCaptor.getValue()).isEqualTo(userStateTopic);
    assertThat(keyCaptor.getValue()).isEqualTo(userId.toString());

    UserState capturedUserState = valueCaptor.getValue();
    assertThat(capturedUserState.getId()).isEqualTo(userId.toString());
    assertThat(capturedUserState.getName()).isEqualTo("Test User");
    assertThat(capturedUserState.getEmail()).isEqualTo("test@example.com");
    assertThat(capturedUserState.getCalendarIds()).hasSize(1);
    assertThat(capturedUserState.getCalendarIds().get(0)).isEqualTo(calendarId.toString());
    assertThat(capturedUserState.getEventType()).isEqualTo(eventType);
  }

  @Test
  @DisplayName("Should handle exception when Kafka send fails")
  void sendUserState_WhenKafkaSendFails_ShouldHandleException() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.createUser(userId, "Test User", "test@example.com", null);
    EventType eventType = EventType.CREATED;

    CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Kafka send failed"));
    when(kafkaTemplate.send(anyString(), anyString(), any(UserState.class))).thenReturn(future);

    // When
    userStateProducer.sendUserState(user, eventType);

    // Then
    verify(kafkaTemplate).send(eq(userStateTopic), eq(userId.toString()), any(UserState.class));
    // No exception should be thrown, as the error is handled in the CompletableFuture callback
  }

  @Test
  @DisplayName("Should set correct event type in user state message")
  void sendUserState_ShouldSetCorrectEventType() {
    // Given
    UUID userId = UUID.randomUUID();
    User user = TestDataFactory.createUser(userId, "Test User", "test@example.com", null);

    CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
    future.complete(sendResult);
    when(kafkaTemplate.send(anyString(), anyString(), any(UserState.class))).thenReturn(future);

    // Test for each event type
    EventType[] eventTypes = {
      EventType.CREATED,
      EventType.UPDATED,
      EventType.DELETED,
      EventType.CALENDAR_ADDED,
      EventType.CALENDAR_REMOVED
    };

    for (EventType eventType : eventTypes) {
      // When
      userStateProducer.sendUserState(user, eventType);

      // Then
      ArgumentCaptor<UserState> valueCaptor = ArgumentCaptor.forClass(UserState.class);
      verify(kafkaTemplate, times(1))
          .send(eq(userStateTopic), eq(userId.toString()), valueCaptor.capture());
      assertThat(valueCaptor.getValue().getEventType()).isEqualTo(eventType);

      // Reset mock for next iteration
      reset(kafkaTemplate);
      // Only stub if not the last iteration
      if (eventType != EventType.CALENDAR_REMOVED) {
        when(kafkaTemplate.send(anyString(), anyString(), any(UserState.class))).thenReturn(future);
      }
    }
  }
}
