package com.ilchern.saasbilling.billing.infrastructure.messaging.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "outbox_messages")
class OutboxMessageEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "aggregatetype", nullable = false)
  var aggregateType: String,

  @Column(name = "aggregateid", nullable = false)
  var aggregateId: String,

  @Column(name = "type", nullable = false)
  var type: String,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  var payload: Map<String, Any>,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "headers", nullable = false, columnDefinition = "jsonb")
  var headers: Map<String, Any>,

  @Column(name = "timestamp", nullable = false)
  var timestamp: LocalDateTime,
)
