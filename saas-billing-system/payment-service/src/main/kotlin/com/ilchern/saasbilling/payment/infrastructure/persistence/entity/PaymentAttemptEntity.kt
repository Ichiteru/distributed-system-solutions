package com.ilchern.saasbilling.payment.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_attempts")
class PaymentAttemptEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "invoice_id", nullable = false)
  var invoiceId: UUID,

  @Column(name = "subscription_id", nullable = false)
  var subscriptionId: UUID,

  @Column(name = "organization_id", nullable = false)
  var organizationId: String,

  @Column(name = "attempt_number", nullable = false)
  var attemptNumber: Int,

  @Column(name = "amount_minor", nullable = false)
  var amountMinor: Long,

  @Column(name = "currency", nullable = false)
  var currency: String,

  @Column(name = "payment_method_token", nullable = false)
  var paymentMethodToken: String,

  @Column(name = "status", nullable = false)
  var status: String,

  @Column(name = "provider_payment_id")
  var providerPaymentId: String?,

  @Column(name = "provider_status")
  var providerStatus: String?,

  @Column(name = "created_at", nullable = false)
  var createdAt: Instant,

  @Column(name = "submitted_at")
  var submittedAt: Instant?,

  @Column(name = "completed_at")
  var completedAt: Instant?,

  @Column(name = "failure_code")
  var failureCode: String?,

  @Column(name = "failure_message")
  var failureMessage: String?,
)
