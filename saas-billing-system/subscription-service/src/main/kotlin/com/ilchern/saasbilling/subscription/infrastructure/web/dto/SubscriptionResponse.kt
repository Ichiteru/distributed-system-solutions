package com.ilchern.saasbilling.subscription.infrastructure.web.dto

import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionStatus
import java.time.Instant
import java.util.UUID

data class SubscriptionResponse(
  val id: UUID,
  val organizationId: String,
  val status: SubscriptionStatus,
  val plan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val createdAt: Instant,
//  val pendingChange: PendingSubscriptionChangeResponse?,
)