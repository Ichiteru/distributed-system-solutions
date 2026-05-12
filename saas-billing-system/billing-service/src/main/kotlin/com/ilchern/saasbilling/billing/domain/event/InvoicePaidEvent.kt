package com.ilchern.saasbilling.billing.domain.event

import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

data class InvoicePaidEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val type: String = TYPE,
  override val invoiceId: InvoiceId,
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val occurredAt: Instant,
  val amount: Money,
  val paidAt: Instant,
) : BillingDomainEvent {
  companion object {
    const val TYPE = "InvoicePaidEvent"
  }
}
