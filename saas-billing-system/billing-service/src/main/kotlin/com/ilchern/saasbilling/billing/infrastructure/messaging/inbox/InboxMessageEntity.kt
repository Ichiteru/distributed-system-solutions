package com.ilchern.saasbilling.billing.infrastructure.messaging.inbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "inbox_messages")
class InboxMessageEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "consumer", nullable = false)
  var consumer: String,

  @Column(name = "message_id", nullable = false)
  var messageId: UUID,

  @Column(name = "message_type", nullable = false)
  var messageType: String,

  @Column(name = "aggregate_id", nullable = false)
  var aggregateId: String,

  @Column(name = "correlation_id")
  var correlationId: UUID?,

  @Column(name = "causation_id")
  var causationId: UUID?,

  @Column(name = "received_at", nullable = false)
  var receivedAt: Instant,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  var payload: Map<String, Any>,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "headers", nullable = false, columnDefinition = "jsonb")
  var headers: Map<String, Any>,
)
