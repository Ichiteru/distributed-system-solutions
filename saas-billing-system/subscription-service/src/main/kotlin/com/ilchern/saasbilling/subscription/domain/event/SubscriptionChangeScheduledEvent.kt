package com.ilchern.saasbilling.subscription.domain.event

import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import java.time.Instant
import java.util.UUID

data class SubscriptionChangeScheduledEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val type: String = TYPE,
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val occurredAt: Instant,
  val newPlan: SubscriptionPlan,
  val newSeats: Int,
) : SubscriptionDomainEvent {
  companion object {
    const val TYPE = "SubscriptionChangeScheduledEvent"
  }
}
