package com.ilchern.saasbilling.orchestrator.application.port

import java.time.Instant
import java.util.UUID

data class CommandOutboxMessage(
  val id: UUID,
  val destinationTopic: String,
  val aggregateType: String,
  val aggregateId: String,
  val type: String,
  val payload: Map<String, Any>,
  val headers: Map<String, Any>,
  val timestamp: Instant,
)
