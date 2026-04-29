package com.ilchern.saasbilling.subscription.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "subscription_changes")
class SubscriptionChangeEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subscription_id", nullable = false, unique = true)
  var subscription: SubscriptionEntity,

  @Column(name = "requested_at", nullable = false)
  var requestedAt: Instant,

  @Column(name = "new_plan", nullable = false)
  var newPlan: String,

  @Column(name = "new_seats", nullable = false)
  var newSeats: Int,
)
