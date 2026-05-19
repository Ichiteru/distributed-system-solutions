package com.ilchern.saasbilling.orchestrator.infrastructure.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import java.time.Clock
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
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
class OrchestratorInfrastructureConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()

  @Bean
  fun orchestratorEventConsumerFactory(
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
  fun orchestratorEventDltProducerFactory(
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
  fun orchestratorEventDltKafkaTemplate(
    orchestratorEventDltProducerFactory: ProducerFactory<String, GenericRecord>,
  ): KafkaTemplate<String, GenericRecord> =
    KafkaTemplate(orchestratorEventDltProducerFactory)

  @Bean
  fun orchestratorEventErrorHandler(
    orchestratorEventDltKafkaTemplate: KafkaTemplate<String, GenericRecord>,
    @Value("\${orchestrator.kafka.topics.subscription-events}") subscriptionEventsTopicName: String,
    @Value("\${orchestrator.kafka.topics.subscription-events-dlt}") subscriptionEventsDltTopicName: String,
    @Value("\${orchestrator.kafka.topics.billing-events}") billingEventsTopicName: String,
    @Value("\${orchestrator.kafka.topics.billing-events-dlt}") billingEventsDltTopicName: String,
    @Value("\${orchestrator.kafka.topics.payment-events}") paymentEventsTopicName: String,
    @Value("\${orchestrator.kafka.topics.payment-events-dlt}") paymentEventsDltTopicName: String,
    @Value("\${orchestrator.kafka.dlt.retry.backoff-ms}") dltRetryBackoffMs: Long,
    @Value("\${orchestrator.kafka.dlt.retry.max-attempts}") dltRetryMaxAttempts: Long,
  ): DefaultErrorHandler {
    val dltTopics = mapOf(
      subscriptionEventsTopicName to subscriptionEventsDltTopicName,
      billingEventsTopicName to billingEventsDltTopicName,
      paymentEventsTopicName to paymentEventsDltTopicName,
    )
    val recoverer = DeadLetterPublishingRecoverer(orchestratorEventDltKafkaTemplate) { record, _ ->
      TopicPartition(
        requireNotNull(dltTopics[record.topic()]) { "DLT topic is not configured for ${record.topic()}" },
        record.partition(),
      )
    }
    return DefaultErrorHandler(recoverer, FixedBackOff(dltRetryBackoffMs, dltRetryMaxAttempts))
  }

  @Bean
  fun orchestratorEventKafkaListenerContainerFactory(
    orchestratorEventConsumerFactory: ConsumerFactory<String, GenericRecord>,
    orchestratorEventErrorHandler: DefaultErrorHandler,
    kafkaProperties: KafkaProperties,
  ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> =
    ConcurrentKafkaListenerContainerFactory<String, GenericRecord>().apply {
      consumerFactory = orchestratorEventConsumerFactory
      setCommonErrorHandler(orchestratorEventErrorHandler)
      setAutoStartup(kafkaProperties.listener.isAutoStartup)
    }
}
