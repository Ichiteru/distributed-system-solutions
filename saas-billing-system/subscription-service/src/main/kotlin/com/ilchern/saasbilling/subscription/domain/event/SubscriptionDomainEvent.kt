package com.ilchern.saasbilling.subscription.domain.event

import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

interface SubscriptionDomainEvent {

  val eventId: UUID
  val type: String
  val subscriptionId: SubscriptionId
  val organizationId: OrganizationId
  val occurredAt: Instant
}
