package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.service.backpressure.OutboundMessagePriority
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder

@Service
class ChatMetrics(
  private val meterRegistry: MeterRegistry,
) {
  private val activeSessions = AtomicInteger(0)
  private val outboundBufferSize = AtomicInteger(0)
  private val acceptedMessages = LongAdder()
  private val rejectedMessages = LongAdder()
  private val deliveryLatencyTimer = Timer.builder("chat.delivery.latency")
    .description("Latency from message creation timestamp to receiver outbound queue acceptance")
    .publishPercentileHistogram()
    .register(meterRegistry)

  init {
    Gauge.builder("chat_ws_sessions_active", activeSessions) { value -> value.get().toDouble() }
      .description("Active WebSocket sessions on this service instance")
      .register(meterRegistry)

    Gauge.builder("chat_outbound_buffer_size", outboundBufferSize) { value -> value.get().toDouble() }
      .description("Total buffered outbound WebSocket events on this service instance")
      .register(meterRegistry)

    Gauge.builder("chat_messages_reject_rate", this, ChatMetrics::currentRejectRate)
      .description("Cumulative rejected message ratio on this service instance")
      .register(meterRegistry)
  }

  fun recordSessionRegistered() {
    activeSessions.incrementAndGet()
  }

  fun recordSessionRemoved() {
    activeSessions.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
  }

  fun incrementOutboundBufferSize() {
    outboundBufferSize.incrementAndGet()
  }

  fun decrementOutboundBufferSize() {
    outboundBufferSize.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
  }

  fun decrementOutboundBufferSize(amount: Int) {
    if (amount <= 0) {
      return
    }

    outboundBufferSize.updateAndGet { current -> (current - amount).coerceAtLeast(0) }
  }

  fun recordOutboundEventDropped(priority: OutboundMessagePriority) {
    meterRegistry.counter(
      "chat_outbound_events_dropped_total",
      "priority",
      priority.name.lowercase(),
    ).increment()
  }

  fun recordMessageAccepted() {
    acceptedMessages.increment()
  }

  fun recordMessageRejected() {
    rejectedMessages.increment()
    meterRegistry.counter("chat_messages_rejected_total").increment()
  }

  fun recordDeliveryLatency(createdAt: Instant) {
    val duration = Duration.between(createdAt, Instant.now())
    if (!duration.isNegative) {
      deliveryLatencyTimer.record(duration.toNanos(), TimeUnit.NANOSECONDS)
    }
  }

  fun recordRedisPublished(channel: String) {
    meterRegistry.counter(
      "chat_redis_events_published_total",
      "channel",
      channel,
    ).increment()
  }

  fun recordRedisConsumed(channel: String) {
    meterRegistry.counter(
      "chat_redis_events_consumed_total",
      "channel",
      channel,
    ).increment()
  }

  private fun currentRejectRate(): Double {
    val accepted = acceptedMessages.sum()
    val rejected = rejectedMessages.sum()
    val total = accepted + rejected

    return if (total == 0L) {
      0.0
    } else {
      rejected.toDouble() / total.toDouble()
    }
  }
}
