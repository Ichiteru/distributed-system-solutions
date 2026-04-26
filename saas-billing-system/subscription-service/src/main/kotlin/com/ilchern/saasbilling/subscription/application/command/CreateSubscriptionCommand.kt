package com.ilchern.saasbilling.subscription.application.command

import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan

data class CreateSubscriptionCommand(
  val organizationId: OrganizationId,
//  val idempotencyKey: String,
  val plan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val paymentMethodToken: PaymentMethodToken,
)
