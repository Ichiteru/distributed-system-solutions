package com.ilchern.saasbilling.billing.domain.event

import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

data class InvoicePaymentPendingEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val type: String = TYPE,
  override val invoiceId: InvoiceId,
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val occurredAt: Instant,
  val amount: Money,
  val paymentPendingAt: Instant,
  val failureCode: String?,
  val failureMessage: String?,
) : BillingDomainEvent {
  companion object {
    const val TYPE = "InvoicePaymentPendingEvent"
  }
}
