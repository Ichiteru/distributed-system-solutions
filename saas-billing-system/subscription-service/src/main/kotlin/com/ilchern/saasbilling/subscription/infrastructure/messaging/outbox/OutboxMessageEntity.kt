package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_messages")
class OutboxMessageEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "message_type", nullable = false)
  var messageType: String,

  @Column(name = "aggregate_id", nullable = false)
  var aggregateId: UUID,

  @Column(name = "aggregate_type", nullable = false)
  var aggregateType: String,

  @Column(name = "correlation_id")
  var correlationId: UUID? = null,

  @Column(name = "causation_id")
  var causationId: UUID? = null,

  @Column(name = "occurred_at", nullable = false)
  var occurredAt: Instant,

  @Column(name = "schema_version", nullable = false)
  var schemaVersion: Int,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  var payload: String,

  @Column(name = "published", nullable = false)
  var published: Boolean = false,

  @Column(name = "published_at")
  var publishedAt: Instant? = null,
)