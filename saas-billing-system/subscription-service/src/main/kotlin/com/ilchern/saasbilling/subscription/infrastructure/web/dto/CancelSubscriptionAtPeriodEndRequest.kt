package com.ilchern.saasbilling.subscription.infrastructure.web.dto

data class CancelSubscriptionAtPeriodEndRequest(
  val reason: String? = null,
)
