package com.ilchern.saasbilling.subscription.infrastructure.persistence.repository

import com.ilchern.saasbilling.subscription.domain.model.Subscription

interface SubscriptionRepository {

  fun save(subscription: Subscription) : Subscription
}