package com.ilchern.saasbilling.subscription.application.port

import com.ilchern.saasbilling.subscription.domain.event.SubscriptionDomainEvent

interface OutboxMessageStore {

  fun append(events: List<SubscriptionDomainEvent>)
}