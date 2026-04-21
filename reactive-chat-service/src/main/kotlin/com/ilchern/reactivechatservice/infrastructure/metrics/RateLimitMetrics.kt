package com.ilchern.reactivechatservice.infrastructure.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class RateLimitMetrics(
  meterRegistry: MeterRegistry,
) {
  private val allowedCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "allowed",
  )
  private val rejectedCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "rejected",
  )
  private val backendErrorCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "backend_error",
  )

  fun recordAllowed() {
    allowedCounter.increment()
  }

  fun recordRejected() {
    rejectedCounter.increment()
  }

  fun recordBackendError() {
    backendErrorCounter.increment()
  }
}
