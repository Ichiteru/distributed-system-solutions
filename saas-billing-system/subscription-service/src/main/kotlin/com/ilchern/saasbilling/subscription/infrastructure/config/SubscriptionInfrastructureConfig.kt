package com.ilchern.saasbilling.subscription.infrastructure.config

import java.time.Clock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
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
        .name(properties.events.name)
        .partitions(properties.events.partitions)
        .replicas(properties.events.replicationFactor)
        .build(),
    )
}
