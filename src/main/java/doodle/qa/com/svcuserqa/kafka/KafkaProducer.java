package doodle.qa.com.svcuserqa.kafka;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for Kafka producers. Provides common functionality for sending messages to Kafka
 * topics.
 */
@RequiredArgsConstructor
@Slf4j
public abstract class KafkaProducer<K, V> {

  protected final KafkaTemplate<K, V> kafkaTemplate;

  /**
   * Sends a message to a Kafka topic.
   *
   * @param topic The Kafka topic to send the message to
   * @param key The message key
   * @param value The message value
   * @return A CompletableFuture that will be completed when the send operation completes
   */
  @Retryable(
      value = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2))
  @Transactional
  protected CompletableFuture<SendResult<K, V>> sendMessage(String topic, K key, V value) {
    log.info("Sending message to Kafka topic: {}, key: {}", topic, key);

    CompletableFuture<SendResult<K, V>> future;

    try {
      future =
          kafkaTemplate.executeInTransaction(
              operations -> {
                CompletableFuture<SendResult<K, V>> result = operations.send(topic, key, value);

                result.whenComplete(
                    (sendResult, ex) -> {
                      if (ex == null) {
                        log.info(
                            "Message sent successfully to topic: {}, key: {}, offset: {}",
                            topic,
                            key,
                            sendResult.getRecordMetadata().offset());
                      } else {
                        log.error("Failed to send message to topic: {}, key: {}", topic, key, ex);
                      }
                    });

                return result;
              });
    } catch (Exception e) {
      log.error("Transaction failed for topic: {}, key: {}", topic, key, e);
      throw e;
    }

    return future;
  }
}
