package com.ilchern.saasbilling.billing.domain.model

import com.ilchern.saasbilling.billing.domain.event.BillingDomainEvent
import com.ilchern.saasbilling.billing.domain.event.InvoiceCreatedEvent
import com.ilchern.saasbilling.billing.domain.event.InvoicePaidEvent
import com.ilchern.saasbilling.billing.domain.event.InvoicePaymentPendingEvent
import java.time.Instant
import java.util.UUID

@JvmInline
value class InvoiceId(val value: UUID)

@JvmInline
value class SubscriptionId(val value: UUID)

@JvmInline
value class OrganizationId(val value: String)

@JvmInline
value class PaymentMethodToken(val value: String)

class Invoice private constructor(
  val id: InvoiceId,
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val invoiceType: InvoiceType,
  private var status: InvoiceStatus,
  val subscriptionPlan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val periodStart: Instant,
  val periodEnd: Instant,
  val amount: Money,
  val paymentMethodToken: PaymentMethodToken,
  val createdAt: Instant,
  private val lines: MutableList<InvoiceLine>,
  private val domainEvents: MutableList<BillingDomainEvent>,
) {

  fun status() = status
  fun lines() = lines.toList()
  fun pullDomainEvents() = domainEvents

  fun markPaid(paidAt: Instant) {
    if (status == InvoiceStatus.PAID) {
      return
    }

    check(status == InvoiceStatus.OPEN || status == InvoiceStatus.PAYMENT_PENDING) {
      "Only open or payment pending invoice can be marked paid"
    }

    status = InvoiceStatus.PAID
    domainEvents.add(
      InvoicePaidEvent(
        invoiceId = id,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        occurredAt = paidAt,
        amount = amount,
        paidAt = paidAt,
      ),
    )
  }

  fun markPaymentPending(
    paymentPendingAt: Instant,
    failureCode: String?,
    failureMessage: String?,
  ) {
    if (status == InvoiceStatus.PAYMENT_PENDING) {
      return
    }

    check(status == InvoiceStatus.OPEN) {
      "Only open invoice can be marked payment pending"
    }

    status = InvoiceStatus.PAYMENT_PENDING
    domainEvents.add(
      InvoicePaymentPendingEvent(
        invoiceId = id,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        occurredAt = paymentPendingAt,
        amount = amount,
        paymentPendingAt = paymentPendingAt,
        failureCode = failureCode,
        failureMessage = failureMessage,
      ),
    )
  }

  companion object {

    fun createInitial(
      subscriptionId: SubscriptionId,
      organizationId: OrganizationId,
      subscriptionPlan: SubscriptionPlan,
      billingPeriod: BillingPeriod,
      seats: Int,
      periodStart: Instant,
      periodEnd: Instant,
      amount: Money,
      paymentMethodToken: PaymentMethodToken,
      createdAt: Instant,
    ): Invoice {
      require(seats > 0) { "Seats must be positive" }
      require(periodEnd.isAfter(periodStart)) { "Invoice period end must be after period start" }

      val invoiceId = InvoiceId(UUID.randomUUID())
      return Invoice(
        id = invoiceId,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        invoiceType = InvoiceType.INITIAL,
        status = InvoiceStatus.OPEN,
        subscriptionPlan = subscriptionPlan,
        billingPeriod = billingPeriod,
        seats = seats,
        periodStart = periodStart,
        periodEnd = periodEnd,
        amount = amount,
        paymentMethodToken = paymentMethodToken,
        createdAt = createdAt,
        lines = mutableListOf(
          InvoiceLine(
            description = "Initial subscription invoice",
            quantity = 1,
            amount = amount,
          ),
        ),
        domainEvents = mutableListOf(
          InvoiceCreatedEvent(
            invoiceId = invoiceId,
            subscriptionId = subscriptionId,
            organizationId = organizationId,
            occurredAt = createdAt,
            invoiceType = InvoiceType.INITIAL,
            status = InvoiceStatus.OPEN,
            subscriptionPlan = subscriptionPlan,
            billingPeriod = billingPeriod,
            seats = seats,
            periodStart = periodStart,
            periodEnd = periodEnd,
            amount = amount,
            paymentMethodToken = paymentMethodToken,
          ),
        ),
      )
    }

    fun copy(
      id: InvoiceId,
      subscriptionId: SubscriptionId,
      organizationId: OrganizationId,
      invoiceType: InvoiceType,
      status: InvoiceStatus,
      subscriptionPlan: SubscriptionPlan,
      billingPeriod: BillingPeriod,
      seats: Int,
      periodStart: Instant,
      periodEnd: Instant,
      amount: Money,
      paymentMethodToken: PaymentMethodToken,
      createdAt: Instant,
      lines: List<InvoiceLine>,
    ): Invoice =
      Invoice(
        id = id,
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        invoiceType = invoiceType,
        status = status,
        subscriptionPlan = subscriptionPlan,
        billingPeriod = billingPeriod,
        seats = seats,
        periodStart = periodStart,
        periodEnd = periodEnd,
        amount = amount,
        paymentMethodToken = paymentMethodToken,
        createdAt = createdAt,
        lines = lines.toMutableList(),
        domainEvents = mutableListOf(),
      )
  }
}
