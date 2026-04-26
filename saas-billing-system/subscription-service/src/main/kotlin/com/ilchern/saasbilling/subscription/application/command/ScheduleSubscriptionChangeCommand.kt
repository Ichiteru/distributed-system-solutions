package com.ilchern.saasbilling.subscription.application.command

import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan

data class ScheduleSubscriptionChangeCommand(
  val subscriptionId: SubscriptionId,
  val newSeats: Int,
  val newPlan: SubscriptionPlan,
)
