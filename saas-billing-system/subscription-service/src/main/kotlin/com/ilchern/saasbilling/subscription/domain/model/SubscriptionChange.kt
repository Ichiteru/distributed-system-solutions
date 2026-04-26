package com.ilchern.saasbilling.subscription.domain.model

import java.time.Instant
import java.util.UUID

data class SubscriptionChange(
  val id: UUID = UUID.randomUUID(),
//  val requestedBy: UserId,
  val requestedAt: Instant,
  val newPlan: SubscriptionPlan,
  val newSeats: Int,
)
