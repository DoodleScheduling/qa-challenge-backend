package doodle.qa.com.svccalendarqa.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;

/** Configuration class for Kafka. */
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  @Value("${spring.retry.kafka.max-attempts}")
  private int maxAttempts;

  @Value("${spring.retry.kafka.initial-interval}")
  private long initialInterval;

  @Value("${spring.retry.kafka.multiplier}")
  private double multiplier;

  @Value("${spring.retry.kafka.max-interval}")
  private long maxInterval;

  private final ConsumerFactory<String, Object> consumerFactory;

  /**
   * Creates a Kafka listener container factory with error handling.
   *
   * @return the Kafka listener container factory
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler());
    factory
        .getContainerProperties()
        .setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    return factory;
  }

  /**
   * Creates a topic for user state events.
   *
   * @return the user state topic
   */
  @Bean
  public NewTopic userStateTopic() {
    return TopicBuilder.name(userStateTopic).partitions(3).replicas(1).build();
  }

  /**
   * Creates an error handler for Kafka listeners.
   *
   * @return the error handler
   */
  @Bean
  public DefaultErrorHandler errorHandler() {
    ExponentialBackOff backOff = new ExponentialBackOffWithMaxRetries(maxAttempts);
    backOff.setInitialInterval(initialInterval);
    backOff.setMultiplier(multiplier);
    backOff.setMaxInterval(maxInterval);

    return new DefaultErrorHandler(backOff);
  }
}
