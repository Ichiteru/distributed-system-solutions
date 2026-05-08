package com.ilchern.saasbilling.billing.domain.model

import java.util.UUID

data class InvoiceLine(
  val id: UUID = UUID.randomUUID(),
  val description: String,
  val quantity: Int,
  val amount: Money,
) {
  init {
    require(description.isNotBlank()) { "Invoice line description must not be blank" }
    require(quantity > 0) { "Invoice line quantity must be positive" }
  }
}
