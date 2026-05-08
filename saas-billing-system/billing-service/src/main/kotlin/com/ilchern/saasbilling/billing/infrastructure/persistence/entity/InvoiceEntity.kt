package com.ilchern.saasbilling.billing.infrastructure.persistence.entity

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
@Table(name = "invoices")
class InvoiceEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @Column(name = "subscription_id", nullable = false)
  var subscriptionId: UUID,

  @Column(name = "organization_id", nullable = false)
  var organizationId: String,

  @Column(name = "invoice_type", nullable = false)
  var invoiceType: String,

  @Column(name = "status", nullable = false)
  var status: String,

  @Column(name = "subscription_plan", nullable = false)
  var subscriptionPlan: String,

  @Column(name = "billing_period", nullable = false)
  var billingPeriod: String,

  @Column(name = "seats", nullable = false)
  var seats: Int,

  @Column(name = "period_start", nullable = false)
  var periodStart: Instant,

  @Column(name = "period_end", nullable = false)
  var periodEnd: Instant,

  @Column(name = "amount_minor", nullable = false)
  var amountMinor: Long,

  @Column(name = "currency", nullable = false)
  var currency: String,

  @Column(name = "payment_method_token", nullable = false)
  var paymentMethodToken: String,

  @Column(name = "created_at", nullable = false)
  var createdAt: Instant,

  @OneToMany(
    mappedBy = "invoice",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    fetch = FetchType.LAZY,
  )
  @OrderBy("id ASC")
  var lines: MutableList<InvoiceLineEntity> = mutableListOf(),
)
