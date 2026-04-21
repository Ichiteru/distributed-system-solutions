package com.ilchern.reactivechatservice.infrastructure.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class WebSocketSessionMetrics(
  meterRegistry: MeterRegistry,
) {
  private val activeSessions = AtomicInteger(0)

  init {
    Gauge.builder("chat_ws_sessions_active", activeSessions) { value -> value.get().toDouble() }
      .description("Active WebSocket sessions on this service instance")
      .register(meterRegistry)
  }

  fun recordRegistered() {
    activeSessions.incrementAndGet()
  }

  fun recordRemoved() {
    activeSessions.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
  }
}
