package com.ilchern.saasbilling.billing.domain.event

import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.InvoiceStatus
import com.ilchern.saasbilling.billing.domain.model.InvoiceType
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import java.time.Instant
import java.util.UUID

data class InvoiceCreatedEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val invoiceId: InvoiceId,
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val occurredAt: Instant,
  val invoiceType: InvoiceType,
  val status: InvoiceStatus,
  val subscriptionPlan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val periodStart: Instant,
  val periodEnd: Instant,
  val amount: Money,
  val paymentMethodToken: PaymentMethodToken,
) : BillingDomainEvent
