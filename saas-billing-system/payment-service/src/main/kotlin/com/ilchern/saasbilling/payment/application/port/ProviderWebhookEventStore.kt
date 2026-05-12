package com.ilchern.saasbilling.payment.application.port

import java.time.Instant

interface ProviderWebhookEventStore {
  fun saveIfAbsent(event: ProviderWebhookEvent): Boolean
}

data class ProviderWebhookEvent(
  val providerEventId: String,
  val providerPaymentId: String,
  val type: String,
  val status: String,
  val receivedAt: Instant,
)
