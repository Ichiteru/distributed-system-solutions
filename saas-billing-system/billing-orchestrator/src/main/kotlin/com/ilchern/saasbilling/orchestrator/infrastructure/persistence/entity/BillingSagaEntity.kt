package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "billing_sagas")
class BillingSagaEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "saga_type", nullable = false)
  var sagaType: String,

  @Column(name = "business_key", nullable = false)
  var businessKey: String,

  @Column(name = "status", nullable = false)
  var status: String,

  @Column(name = "correlation_id")
  var correlationId: UUID?,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
  var metadata: Map<String, Any>,

  @Column(name = "started_at", nullable = false)
  var startedAt: Instant,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: Instant,

  @Column(name = "completed_at")
  var completedAt: Instant?,
)
