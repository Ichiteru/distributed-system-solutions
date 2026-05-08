package com.ilchern.saasbilling.billing.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "invoice_lines")
class InvoiceLineEntity(
  @Id
  @Column(name = "id", nullable = false)
  var id: UUID,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  var invoice: InvoiceEntity,

  @Column(name = "description", nullable = false)
  var description: String,

  @Column(name = "quantity", nullable = false)
  var quantity: Int,

  @Column(name = "amount_minor", nullable = false)
  var amountMinor: Long,

  @Column(name = "currency", nullable = false)
  var currency: String,
)
