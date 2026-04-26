package com.ilchern.saasbilling.subscription.infrastructure.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "subscriptions")
class SubscriptionEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "organization_id", nullable = false)
  var organizationId: String,

  @Column(name = "created_at", nullable = false)
  var createdAt: Instant,

  @Column(name = "status", nullable = false)
  var status: String,

  @Column(name = "subscription_plan", nullable = false)
  var subscriptionPlan: String,

  @Column(name = "billing_period", nullable = false)
  var billingPeriod: String,

  @Column(name = "seats", nullable = false)
  var seats: Int,

  @Column(name = "payment_method_token", nullable = false)
  var paymentMethodToken: String,

  @OneToMany(
    mappedBy = "subscription",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    fetch = FetchType.LAZY,
  )
  @OrderBy("requestedAt DESC, id DESC")
  var subscriptionChanges: MutableList<SubscriptionChangeEntity> = mutableListOf(),

  @OneToMany(
    mappedBy = "subscription",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    fetch = FetchType.LAZY,
  )
  @OrderBy("occurredAt ASC, id ASC")
  var historyEntries: MutableList<SubscriptionHistoryEntryEntity> = mutableListOf(),
)