package com.ilchern.saasbilling.messaging.outbox

import java.time.Instant
import java.util.UUID

data class OutboxMessage(
  val id: UUID,
  val aggregateType: String,
  val aggregateId: String,
  val type: String,
  val payload: Map<String, Any>,
  val headers: Map<String, Any>,
  val occurredAt: Instant,
)
