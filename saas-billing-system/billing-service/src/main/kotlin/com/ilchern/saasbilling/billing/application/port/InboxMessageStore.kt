package com.ilchern.saasbilling.billing.application.port

import java.time.Instant
import java.util.UUID

data class InboxMessage(
  val consumer: String,
  val messageId: UUID,
  val messageType: String,
  val aggregateId: String,
  val correlationId: UUID?,
  val causationId: UUID?,
  val receivedAt: Instant,
  val payload: Map<String, Any>,
  val headers: Map<String, Any>,
)

interface InboxMessageStore {

  fun saveIfAbsent(message: InboxMessage): Boolean
}
