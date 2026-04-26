package com.ilchern.saasbilling.subscription.infrastructure.web.dto

import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ScheduleSubscriptionChangeRequest(
  @field:Min(1)
  val newSeats: Int,

  @field:NotNull
  val newPlan: SubscriptionPlan?,
)
