package com.ilchern.saasbilling.billing.domain.event

import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

interface BillingDomainEvent {

  val eventId: UUID
  val type: String
  val invoiceId: InvoiceId
  val subscriptionId: SubscriptionId
  val organizationId: OrganizationId
  val occurredAt: Instant
}
