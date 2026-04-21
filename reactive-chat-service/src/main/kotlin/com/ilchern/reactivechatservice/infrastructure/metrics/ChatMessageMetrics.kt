package com.ilchern.reactivechatservice.infrastructure.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.LongAdder

@Component
class ChatMessageMetrics(
  private val meterRegistry: MeterRegistry,
) {
  private val acceptedMessages = LongAdder()
  private val rejectedMessages = LongAdder()

  init {
    Gauge.builder("chat_messages_reject_rate", this, ChatMessageMetrics::currentRejectRate)
      .description("Cumulative rejected message ratio on this service instance")
      .register(meterRegistry)
  }

  fun recordAccepted() {
    acceptedMessages.increment()
  }

  fun recordRejected() {
    rejectedMessages.increment()
    meterRegistry.counter("chat_messages_rejected_total").increment()
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
