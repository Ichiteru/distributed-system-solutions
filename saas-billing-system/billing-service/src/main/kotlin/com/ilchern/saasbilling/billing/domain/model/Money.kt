package com.ilchern.saasbilling.billing.domain.model

data class Money(
  val amountMinor: Long,
  val currency: String,
) {
  init {
    require(amountMinor >= 0) { "Amount must not be negative" }
    require(currency.isNotBlank()) { "Currency must not be blank" }
  }
}
