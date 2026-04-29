package com.ilchern.saasbilling.subscription.infrastructure.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import java.time.Clock
import org.apache.avro.specific.SpecificRecord
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaAdmin.NewTopics

@Configuration
@EnableConfigurationProperties(SubscriptionKafkaTopicsProperties::class)
class SubscriptionInfrastructureConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()

  @Bean
  fun subscriptionTopics(properties: SubscriptionKafkaTopicsProperties): NewTopics =
    NewTopics(
      TopicBuilder
        .name(properties.commands.name)
        .partitions(properties.commands.partitions)
        .replicas(properties.commands.replicationFactor)
        .build(),
      TopicBuilder
        .name(properties.events.name)
        .partitions(properties.events.partitions)
        .replicas(properties.events.replicationFactor)
        .build(),
    )

  @Bean
  fun subscriptionCommandConsumerFactory(
    kafkaProperties: KafkaProperties,
  ): ConsumerFactory<String, SpecificRecord> {
    val consumerProperties = kafkaProperties.buildConsumerProperties().toMutableMap()
    consumerProperties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
    require(consumerProperties.containsKey(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)) {
      "schema.registry.url must be configured for Avro consumers"
    }
    return DefaultKafkaConsumerFactory(consumerProperties)
  }

  @Bean
  fun subscriptionCommandKafkaListenerContainerFactory(
    subscriptionCommandConsumerFactory: ConsumerFactory<String, SpecificRecord>,
  ): ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> =
    ConcurrentKafkaListenerContainerFactory<String, SpecificRecord>().apply {
      consumerFactory = subscriptionCommandConsumerFactory
    }
}
