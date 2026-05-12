package com.ilchern.saasbilling.payment.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_webhook_events")
class ProviderWebhookEventEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "provider_event_id", nullable = false)
  var providerEventId: String,

  @Column(name = "provider_payment_id", nullable = false)
  var providerPaymentId: String,

  @Column(name = "type", nullable = false)
  var type: String,

  @Column(name = "status", nullable = false)
  var status: String,

  @Column(name = "received_at", nullable = false)
  var receivedAt: Instant,
)
