package com.ilchern.saasbilling.payment.domain.model

import com.ilchern.saasbilling.payment.domain.event.PaymentDomainEvent
import com.ilchern.saasbilling.payment.domain.event.PaymentFailedEvent
import com.ilchern.saasbilling.payment.domain.event.PaymentSucceededEvent
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
  private var completedAt: Instant?,
  private var failureCode: String?,
  private var failureMessage: String?,
  private val domainEvents: MutableList<PaymentDomainEvent>,
) {

  fun status(): PaymentAttemptStatus = status
  fun pullDomainEvents(): List<PaymentDomainEvent> = domainEvents.toList()
  fun providerPaymentReference(): ProviderPaymentReference? = providerPaymentReference
  fun providerStatus(): String? = providerStatus
  fun submittedAt(): Instant? = submittedAt
  fun completedAt(): Instant? = completedAt
  fun failureCode(): String? = failureCode
  fun failureMessage(): String? = failureMessage
  fun isCreated(): Boolean = status == PaymentAttemptStatus.CREATED
  fun canStartNextAttempt(): Boolean =
    status == PaymentAttemptStatus.FAILED || status == PaymentAttemptStatus.TIMED_OUT
  fun isTerminal(): Boolean =
    status == PaymentAttemptStatus.SUCCEEDED ||
      status == PaymentAttemptStatus.FAILED ||
      status == PaymentAttemptStatus.TIMED_OUT

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

  fun markSucceeded(
    providerStatus: String,
    completedAt: Instant,
  ) {
    check(!isTerminal()) { "Payment attempt already has terminal outcome" }
    check(status == PaymentAttemptStatus.SUBMITTED) { "Only submitted payment attempt can succeed" }
    this.status = PaymentAttemptStatus.SUCCEEDED
    this.providerStatus = providerStatus
    this.completedAt = completedAt
    this.failureCode = null
    this.failureMessage = null
    this.domainEvents.add(
      PaymentSucceededEvent(
        paymentAttemptId = id,
        invoiceId = invoiceId,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        amount = amount,
        providerPaymentReference = requireNotNull(providerPaymentReference),
        attemptNumber = attemptNumber,
        occurredAt = completedAt,
      ),
    )
  }

  fun markFailed(
    providerStatus: String,
    failureCode: String?,
    failureMessage: String?,
    completedAt: Instant,
  ) {
    check(!isTerminal()) { "Payment attempt already has terminal outcome" }
    check(status == PaymentAttemptStatus.SUBMITTED) { "Only submitted payment attempt can fail" }
    this.status = PaymentAttemptStatus.FAILED
    this.providerStatus = providerStatus
    this.completedAt = completedAt
    this.failureCode = failureCode
    this.failureMessage = failureMessage
    this.domainEvents.add(
      PaymentFailedEvent(
        paymentAttemptId = id,
        invoiceId = invoiceId,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        amount = amount,
        providerPaymentReference = requireNotNull(providerPaymentReference),
        attemptNumber = attemptNumber,
        occurredAt = completedAt,
        failureCode = failureCode,
        failureMessage = failureMessage,
      ),
    )
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
        completedAt = null,
        failureCode = null,
        failureMessage = null,
        domainEvents = mutableListOf(),
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
      completedAt: Instant?,
      failureCode: String?,
      failureMessage: String?,
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
        completedAt = completedAt,
        failureCode = failureCode,
        failureMessage = failureMessage,
        domainEvents = mutableListOf(),
      )
  }
}
