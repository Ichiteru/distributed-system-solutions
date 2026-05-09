package com.ilchern.saasbilling.orchestrator.domain.model

import java.time.Instant
import java.util.UUID

class BillingSaga private constructor(
  val id: UUID,
  val sagaType: String,
  val businessKey: String,
  private var status: String,
  val correlationId: UUID?,
  private val metadata: MutableMap<String, Any>,
  val startedAt: Instant,
  private var updatedAt: Instant,
  private var completedAt: Instant?,
) {

  fun status(): String = status

  fun metadata(): Map<String, Any> = metadata.toMap()

  fun updatedAt(): Instant = updatedAt

  fun completedAt(): Instant? = completedAt

  fun mergeMetadata(values: Map<String, Any>) {
    metadata.putAll(values)
  }

  fun transitionTo(
    newStatus: String,
    terminal: Boolean,
    occurredAt: Instant,
  ) {
    status = newStatus
    updatedAt = occurredAt
    completedAt = occurredAt.takeIf { terminal }
  }

  companion object {

    fun start(
      sagaType: String,
      businessKey: String,
      initialStatus: String,
      correlationId: UUID?,
      metadata: Map<String, Any>,
      startedAt: Instant,
    ): BillingSaga =
      BillingSaga(
        id = UUID.randomUUID(),
        sagaType = sagaType,
        businessKey = businessKey,
        status = initialStatus,
        correlationId = correlationId,
        metadata = metadata.toMutableMap(),
        startedAt = startedAt,
        updatedAt = startedAt,
        completedAt = null,
      )

    fun restore(
      id: UUID,
      sagaType: String,
      businessKey: String,
      status: String,
      correlationId: UUID?,
      metadata: Map<String, Any>,
      startedAt: Instant,
      updatedAt: Instant,
      completedAt: Instant?,
    ): BillingSaga =
      BillingSaga(
        id = id,
        sagaType = sagaType,
        businessKey = businessKey,
        status = status,
        correlationId = correlationId,
        metadata = metadata.toMutableMap(),
        startedAt = startedAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
      )
  }
}
