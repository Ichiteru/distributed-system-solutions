package com.ilchern.saasbilling.subscription.application.command

import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

data class CompleteSubscriptionCancellationCommand(
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val messageId: UUID,
  val correlationId: UUID?,
  val causationId: UUID?,
  val occurredAt: Instant,
)
