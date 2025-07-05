package doodle.qa.com.svccalendarqa.config;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Test configuration for Kafka components. This configuration provides beans for testing with
 * embedded Kafka and mock schema registry.
 */
@TestConfiguration
public class TestConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Creates a mock schema registry client for testing.
   *
   * @return A mock schema registry client
   */
  @Bean
  public SchemaRegistryClient schemaRegistryClient() {
    return new MockSchemaRegistryClient();
  }

  /**
   * Creates a consumer factory for testing with Avro deserialization.
   *
   * @return A consumer factory configured for testing
   */
  @Bean
  @Primary
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
    props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://testcontainers");
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

    return new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), new KafkaAvroDeserializer(schemaRegistryClient()));
  }

  /**
   * Creates a producer factory for testing with Avro serialization.
   *
   * @return A producer factory configured for testing
   */
  @Bean
  @Primary
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://testcontainers");
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

    return new DefaultKafkaProducerFactory<>(
        props, new StringSerializer(), new KafkaAvroSerializer(schemaRegistryClient()));
  }

  /**
   * Creates a Kafka template for testing.
   *
   * @return A Kafka template configured for testing
   */
  @Bean
  @Primary
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
