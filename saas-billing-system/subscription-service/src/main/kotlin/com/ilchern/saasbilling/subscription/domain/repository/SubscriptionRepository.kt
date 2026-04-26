package com.ilchern.saasbilling.subscription.domain.repository

import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId

interface SubscriptionRepository {

  fun save(subscription: Subscription): Subscription
  fun findById(subscriptionId: SubscriptionId): Subscription?
}
