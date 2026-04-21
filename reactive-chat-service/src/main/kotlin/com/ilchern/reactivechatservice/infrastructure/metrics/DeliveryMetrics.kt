package com.ilchern.reactivechatservice.infrastructure.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class DeliveryMetrics(
  meterRegistry: MeterRegistry,
) {
  private val deliveryLatencyTimer = Timer.builder("chat.delivery.latency")
    .description("Latency from message creation timestamp to receiver outbound queue acceptance")
    .publishPercentileHistogram()
    .register(meterRegistry)

  fun recordDeliveryLatency(createdAt: Instant) {
    val duration = Duration.between(createdAt, Instant.now())
    if (!duration.isNegative) {
      deliveryLatencyTimer.record(duration.toNanos(), TimeUnit.NANOSECONDS)
    }
  }
}
