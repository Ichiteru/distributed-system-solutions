package com.ilchern.saasbilling.subscription.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
  name = "idempotency_keys",
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_idempotency_keys_scope",
      columnNames = ["organization_id", "operation", "idempotency_key"],
    ),
  ],
)
class IdempotencyKeyEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "organization_id", nullable = false)
  var organizationId: String,

  @Column(name = "operation", nullable = false)
  var operation: String,

  @Column(name = "idempotency_key", nullable = false)
  var idempotencyKey: String,

  @Column(name = "subscription_id", nullable = false)
  var subscriptionId: UUID,

  @Column(name = "created_at", nullable = false)
  var createdAt: Instant,
)
