package com.ilchern.saasbilling.subscription.infrastructure.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import java.time.Clock
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class SubscriptionInfrastructureConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()

  @Bean
  fun subscriptionCommandConsumerFactory(
    kafkaProperties: KafkaProperties,
  ): ConsumerFactory<String, GenericRecord> {
    val consumerProperties = kafkaProperties.buildConsumerProperties().toMutableMap()
    consumerProperties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = false
    require(consumerProperties.containsKey(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)) {
      "schema.registry.url must be configured for Avro consumers"
    }
    return DefaultKafkaConsumerFactory(consumerProperties)
  }

  @Bean
  fun subscriptionCommandDltProducerFactory(
    kafkaProperties: KafkaProperties,
  ): ProducerFactory<String, GenericRecord> {
    val producerProperties = kafkaProperties.buildProducerProperties().toMutableMap()
    producerProperties[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    producerProperties[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
    require(producerProperties.containsKey(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)) {
      "schema.registry.url must be configured for Avro DLT producers"
    }
    return DefaultKafkaProducerFactory(producerProperties)
  }

  @Bean
  fun subscriptionCommandDltKafkaTemplate(
    subscriptionCommandDltProducerFactory: ProducerFactory<String, GenericRecord>,
  ): KafkaTemplate<String, GenericRecord> =
    KafkaTemplate(subscriptionCommandDltProducerFactory)

  @Bean
  fun subscriptionCommandErrorHandler(
    subscriptionCommandDltKafkaTemplate: KafkaTemplate<String, GenericRecord>,
    @Value("\${subscription.kafka.topics.commands-dlt.name}") subscriptionCommandDltTopicName: String,
    @Value("\${subscription.kafka.dlt.retry.backoff-ms}") dltRetryBackoffMs: Long,
    @Value("\${subscription.kafka.dlt.retry.max-attempts}") dltRetryMaxAttempts: Long,
  ): DefaultErrorHandler {
    val recoverer = DeadLetterPublishingRecoverer(subscriptionCommandDltKafkaTemplate) { record, _ ->
      TopicPartition(subscriptionCommandDltTopicName, record.partition())
    }
    return DefaultErrorHandler(recoverer, FixedBackOff(dltRetryBackoffMs, dltRetryMaxAttempts))
  }

  @Bean
  fun subscriptionCommandKafkaListenerContainerFactory(
    subscriptionCommandConsumerFactory: ConsumerFactory<String, GenericRecord>,
    kafkaProperties: KafkaProperties,
    subscriptionCommandErrorHandler: DefaultErrorHandler,
  ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> =
    ConcurrentKafkaListenerContainerFactory<String, GenericRecord>().apply {
      consumerFactory = subscriptionCommandConsumerFactory
      setCommonErrorHandler(subscriptionCommandErrorHandler)
      setAutoStartup(kafkaProperties.listener.isAutoStartup)
    }
}
