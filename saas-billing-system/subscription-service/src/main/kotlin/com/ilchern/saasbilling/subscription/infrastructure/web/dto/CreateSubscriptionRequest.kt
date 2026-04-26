package com.ilchern.saasbilling.subscription.infrastructure.web.dto

import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateSubscriptionRequest(
  @field:NotNull
  val plan: SubscriptionPlan?,

  @field:NotNull
  val billingPeriod: BillingPeriod?,

  @field:Min(1)
  val seats: Int,

  @field:NotBlank
  @field:NotNull
  val paymentMethodToken: String,
)
