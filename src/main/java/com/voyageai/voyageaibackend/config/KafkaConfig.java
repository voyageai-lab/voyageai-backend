package com.voyageai.voyageaibackend.config;

import com.voyageai.voyageaibackend.kafka.event.PlanningProgressEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningRequestEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningResultEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Kafka configuration for the planning event pipeline.
 *
 * <p>Three topics form the async planning pipeline:
 * <ol>
 *   <li>{@code planning.request} - Java produces, Python consumes</li>
 *   <li>{@code planning.progress} - Python produces, Java consumes</li>
 *   <li>{@code planning.result} - Python produces, Java consumes</li>
 * </ol>
 *
 * <p>Serialization: JSON (Jackson) for readability and debuggability.
 * In production with high throughput, consider Avro + Schema Registry.
 *
 * <p>Partition strategy: taskId as key ensures ordering per task.
 */
@Configuration
@Slf4j
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id:voyageai-java}")
  private String consumerGroupId;

  @Value("${kafka.topic.planning.request:planning.request}")
  private String requestTopic;

  @Value("${kafka.topic.planning.progress:planning.progress}")
  private String progressTopic;

  @Value("${kafka.topic.planning.result:planning.result}")
  private String resultTopic;

  // =========================================================================
  // Topic Auto-Creation
  // =========================================================================

  @Bean
  public NewTopic planningRequestTopic() {
    return TopicBuilder.name(requestTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic planningProgressTopic() {
    return TopicBuilder.name(progressTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic planningResultTopic() {
    return TopicBuilder.name(resultTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  // =========================================================================
  // Producer Configuration (Java -> Kafka)
  // =========================================================================

  @Bean
  public ProducerFactory<String, PlanningRequestEvent> requestProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    // Ensure all replicas acknowledge for durability
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    // Enable idempotent producer to avoid duplicate sends on retry
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, PlanningRequestEvent> requestKafkaTemplate() {
    return new KafkaTemplate<>(requestProducerFactory());
  }

  // =========================================================================
  // Consumer Configuration (Kafka -> Java)
  // =========================================================================

  @Bean
  public ConsumerFactory<String, PlanningProgressEvent> progressConsumerFactory() {
    Map<String, Object> props = consumerBaseProps();
    JsonDeserializer<PlanningProgressEvent> deserializer =
        new JsonDeserializer<>(PlanningProgressEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.setUseTypeHeaders(false);
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PlanningProgressEvent>
      progressListenerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, PlanningProgressEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(progressConsumerFactory());
    factory.setConcurrency(2);
    return factory;
  }

  @Bean
  public ConsumerFactory<String, PlanningResultEvent> resultConsumerFactory() {
    Map<String, Object> props = consumerBaseProps();
    JsonDeserializer<PlanningResultEvent> deserializer =
        new JsonDeserializer<>(PlanningResultEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.setUseTypeHeaders(false);
    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PlanningResultEvent>
      resultListenerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, PlanningResultEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(resultConsumerFactory());
    factory.setConcurrency(2);
    return factory;
  }

  private Map<String, Object> consumerBaseProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    return props;
  }
}
