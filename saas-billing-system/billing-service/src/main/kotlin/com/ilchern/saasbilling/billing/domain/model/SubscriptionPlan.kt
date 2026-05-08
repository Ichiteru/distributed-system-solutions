package com.ilchern.saasbilling.billing.domain.model

enum class SubscriptionPlan(
  val basePriceMinor: Long,
  val seatPriceMinor: Long,
) {
  BASIC(basePriceMinor = 1000, seatPriceMinor = 100),
  PRO(basePriceMinor = 2000, seatPriceMinor = 200),
  ENTERPRISE(basePriceMinor = 3000, seatPriceMinor = 300),
}
