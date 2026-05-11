package com.ilchern.saasbilling.payment.domain.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class PaymentAttemptId(val value: UUID)

@JvmInline
value class InvoiceId(val value: UUID)

@JvmInline
value class SubscriptionId(val value: UUID)

@JvmInline
value class OrganizationId(val value: String)

@JvmInline
value class PaymentMethodToken(val value: String)

@JvmInline
value class ProviderPaymentReference(val value: String)

data class Money(
  val amountMinor: Long,
  val currency: String,
) {
  init {
    require(amountMinor > 0) { "Payment amount must be positive" }
    require(currency.isNotBlank()) { "Payment currency must not be blank" }
  }
}

class PaymentAttempt private constructor(
  val id: PaymentAttemptId,
  val invoiceId: InvoiceId,
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val attemptNumber: Int,
  val amount: Money,
  val paymentMethodToken: PaymentMethodToken,
  private var status: PaymentAttemptStatus,
  private var providerPaymentReference: ProviderPaymentReference?,
  private var providerStatus: String?,
  val createdAt: Instant,
  private var submittedAt: Instant?,
) {

  fun status(): PaymentAttemptStatus = status
  fun providerPaymentReference(): ProviderPaymentReference? = providerPaymentReference
  fun providerStatus(): String? = providerStatus
  fun submittedAt(): Instant? = submittedAt
  fun isCreated(): Boolean = status == PaymentAttemptStatus.CREATED
  fun canStartNextAttempt(): Boolean =
    status == PaymentAttemptStatus.FAILED || status == PaymentAttemptStatus.TIMED_OUT

  fun markSubmitted(
    providerPaymentReference: ProviderPaymentReference,
    providerStatus: String,
    submittedAt: Instant,
  ) {
    check(status == PaymentAttemptStatus.CREATED) { "Only created payment attempt can be submitted" }
    this.status = PaymentAttemptStatus.SUBMITTED
    this.providerPaymentReference = providerPaymentReference
    this.providerStatus = providerStatus
    this.submittedAt = submittedAt
  }

  companion object {

    fun create(
      invoiceId: InvoiceId,
      subscriptionId: SubscriptionId,
      organizationId: OrganizationId,
      attemptNumber: Int,
      amount: Money,
      paymentMethodToken: PaymentMethodToken,
      createdAt: Instant,
    ): PaymentAttempt {
      require(attemptNumber > 0) { "Payment attempt number must be positive" }
      require(paymentMethodToken.value.isNotBlank()) { "Payment method token must not be blank" }

      return PaymentAttempt(
        id = PaymentAttemptId(UUID.randomUUID()),
        invoiceId = invoiceId,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        attemptNumber = attemptNumber,
        amount = amount,
        paymentMethodToken = paymentMethodToken,
        status = PaymentAttemptStatus.CREATED,
        providerPaymentReference = null,
        providerStatus = null,
        createdAt = createdAt,
        submittedAt = null,
      )
    }

    fun restore(
      id: PaymentAttemptId,
      invoiceId: InvoiceId,
      subscriptionId: SubscriptionId,
      organizationId: OrganizationId,
      attemptNumber: Int,
      amount: Money,
      paymentMethodToken: PaymentMethodToken,
      status: PaymentAttemptStatus,
      providerPaymentReference: ProviderPaymentReference?,
      providerStatus: String?,
      createdAt: Instant,
      submittedAt: Instant?,
    ): PaymentAttempt =
      PaymentAttempt(
        id = id,
        invoiceId = invoiceId,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        attemptNumber = attemptNumber,
        amount = amount,
        paymentMethodToken = paymentMethodToken,
        status = status,
        providerPaymentReference = providerPaymentReference,
        providerStatus = providerStatus,
        createdAt = createdAt,
        submittedAt = submittedAt,
      )
  }
}
