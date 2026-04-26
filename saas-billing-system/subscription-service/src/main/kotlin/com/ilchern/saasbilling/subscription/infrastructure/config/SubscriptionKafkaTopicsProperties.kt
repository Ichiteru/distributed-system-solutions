package com.ilchern.saasbilling.subscription.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "subscription.kafka.topics")
data class SubscriptionKafkaTopicsProperties(
  val events: TopicProperties,
) {
  data class TopicProperties(
    val name: String,
    val partitions: Int,
    val replicationFactor: Int,
  )
}
