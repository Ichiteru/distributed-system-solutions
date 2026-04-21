package com.ilchern.reactivechatservice.infrastructure.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class RedisPubSubMetrics(
  private val meterRegistry: MeterRegistry,
) {

  fun recordPublished(channel: String) {
    meterRegistry.counter(
      "chat_redis_events_published_total",
      "channel",
      channel,
    ).increment()
  }

  fun recordConsumed(channel: String) {
    meterRegistry.counter(
      "chat_redis_events_consumed_total",
      "channel",
      channel,
    ).increment()
  }
}
