package com.ilchern.saasbilling.subscription.domain.model

enum class SubscriptionStatus{
  PENDING, ACTIVE, PAST_DUE, SUSPENDED, CANCEL_AT_PERIOD_END, CANCELED
}