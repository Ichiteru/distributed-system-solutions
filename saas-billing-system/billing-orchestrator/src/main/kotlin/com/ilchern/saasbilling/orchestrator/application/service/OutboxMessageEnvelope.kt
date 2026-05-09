package com.ilchern.saasbilling.orchestrator.application.service

import java.time.Instant
import java.util.UUID

data class OutboxMessageEnvelope(
  val id: UUID,
  val type: String,
  val aggregateType: String,
  val aggregateId: String,
  val timestamp: Instant,
  val headers: Map<String, Any>,
  val payload: Map<String, Any>,
)
