package doodle.qa.com.svcuserqa.config;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

  @Value("${kafka.topics.user-state}")
  private String userStateTopic;

  @Value("${kafka.topics.user-state-dlt}")
  private String userStateDltTopic;

  @Value("${spring.retry.kafka.max-attempts}")
  private int maxAttempts;

  @Value("${spring.retry.kafka.initial-interval}")
  private long initialInterval;

  @Value("${spring.retry.kafka.multiplier}")
  private double multiplier;

  @Value("${spring.retry.kafka.max-interval}")
  private long maxInterval;

  private final ProducerFactory<String, Object> producerFactory;

  @Bean
  public NewTopic userStateTopic() {
    return TopicBuilder.name(userStateTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic userStateDltTopic() {
    return TopicBuilder.name(userStateDltTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-");

    KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory, props);
    kafkaTemplate.setTransactionIdPrefix("tx-");
    return kafkaTemplate;
  }

  @Bean
  public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    ExponentialBackOff backOff = new ExponentialBackOffWithMaxRetries(maxAttempts);
    backOff.setInitialInterval(initialInterval);
    backOff.setMultiplier(multiplier);
    backOff.setMaxInterval(maxInterval);

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> {
              String originalTopic = record.topic();
              return new TopicPartitionOffset(
                      originalTopic + ".DLT", record.partition(), record.offset())
                  .getTopicPartition();
            });

    return new DefaultErrorHandler(recoverer, backOff);
  }
}
