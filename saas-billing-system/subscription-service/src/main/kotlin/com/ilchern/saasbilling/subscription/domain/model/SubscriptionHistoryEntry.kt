package com.ilchern.saasbilling.subscription.domain.model

import java.time.Instant
import java.util.UUID

data class SubscriptionHistoryEntry(
  val id: UUID = UUID.randomUUID(),
  val action: String,
//  val actorUserId: UserId?,
  val occurredAt: Instant,
  val details: Map<String, String> = emptyMap(),
)
