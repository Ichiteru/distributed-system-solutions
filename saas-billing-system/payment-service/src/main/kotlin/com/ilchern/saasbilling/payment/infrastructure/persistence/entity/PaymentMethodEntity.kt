package com.ilchern.saasbilling.payment.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_methods")
class PaymentMethodEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "organization_id", nullable = false)
  var organizationId: String,

  @Column(name = "token", nullable = false)
  var token: String,

  @Column(name = "created_at", nullable = false)
  var createdAt: Instant,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: Instant,
)
