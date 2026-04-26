package com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka

import com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox.OutboxMessageEntity

interface SubscriptionEventPublisher {

  fun publish(message: OutboxMessageEntity)
}
