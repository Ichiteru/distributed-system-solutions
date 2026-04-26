package com.ilchern.saasbilling.subscription.domain.model

enum class SubscriptionPlan(
  val basePriceUsd: Double,
  val seatPriceUsd: Double,
){
  // todo вынести в конфиг значения
  BASIC(basePriceUsd = 1000.0, seatPriceUsd = 100.0),
  PRO(basePriceUsd = 2000.0, seatPriceUsd = 200.0),
  ENTERPRISE(basePriceUsd = 3000.0, seatPriceUsd = 300.0),
}