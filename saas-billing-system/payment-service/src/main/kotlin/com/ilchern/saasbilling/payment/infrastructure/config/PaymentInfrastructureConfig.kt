package com.ilchern.saasbilling.payment.infrastructure.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import java.time.Clock
import org.apache.avro.generic.GenericRecord
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
class PaymentInfrastructureConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()

  @Bean
  fun paymentCommandConsumerFactory(
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
  fun paymentCommandKafkaListenerContainerFactory(
    paymentCommandConsumerFactory: ConsumerFactory<String, GenericRecord>,
  ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> =
    ConcurrentKafkaListenerContainerFactory<String, GenericRecord>().apply {
      consumerFactory = paymentCommandConsumerFactory
    }
}
