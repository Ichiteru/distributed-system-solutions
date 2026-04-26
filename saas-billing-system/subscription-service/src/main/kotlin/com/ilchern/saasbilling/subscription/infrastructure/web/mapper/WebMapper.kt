package com.ilchern.saasbilling.subscription.infrastructure.web.mapper

import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.SubscriptionResponse

fun Subscription.toResponse(): SubscriptionResponse =
  SubscriptionResponse(
    id = id.value,
    organizationId = organizationId.value,
    status = status(),
    plan = subscriptionPlan(),
    billingPeriod = billingPeriod(),
    seats = seats(),
    createdAt = createdAt,
  )