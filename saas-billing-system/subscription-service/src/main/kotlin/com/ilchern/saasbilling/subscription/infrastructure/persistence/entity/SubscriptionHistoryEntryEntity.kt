package com.ilchern.saasbilling.subscription.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "subscription_history")
class SubscriptionHistoryEntryEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subscription_id", nullable = false)
  var subscription: SubscriptionEntity,

  @Column(name = "action", nullable = false)
  var action: String,

  @Column(name = "occurred_at", nullable = false)
  var occurredAt: Instant,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = false, columnDefinition = "jsonb")
  var details: MutableMap<String, String> = mutableMapOf(),
) {
  constructor() : this(
    id = UUID.randomUUID(),
    subscription = SubscriptionEntity(),
    action = "",
    occurredAt = Instant.EPOCH,
  )
}
