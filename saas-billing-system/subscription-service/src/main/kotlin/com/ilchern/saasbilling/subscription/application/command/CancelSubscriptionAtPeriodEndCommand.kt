package com.ilchern.saasbilling.subscription.application.command

import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId

data class CancelSubscriptionAtPeriodEndCommand(
  val subscriptionId: SubscriptionId,
)
