package com.ilchern.reactivechatservice.infrastructure.metrics

import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class OutboundBufferMetrics(
  private val meterRegistry: MeterRegistry,
) {
  private val outboundBufferSize = AtomicInteger(0)

  init {
    Gauge.builder("chat_outbound_buffer_size", outboundBufferSize) { value -> value.get().toDouble() }
      .description("Total buffered outbound WebSocket events on this service instance")
      .register(meterRegistry)
  }

  fun incrementBufferSize() {
    outboundBufferSize.incrementAndGet()
  }

  fun decrementBufferSize() {
    outboundBufferSize.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
  }

  fun decrementBufferSize(amount: Int) {
    if (amount <= 0) {
      return
    }

    outboundBufferSize.updateAndGet { current -> (current - amount).coerceAtLeast(0) }
  }

  fun recordEventDropped(priority: OutboundMessagePriority) {
    meterRegistry.counter(
      "chat_outbound_events_dropped_total",
      "priority",
      priority.name.lowercase(),
    ).increment()
  }
}
