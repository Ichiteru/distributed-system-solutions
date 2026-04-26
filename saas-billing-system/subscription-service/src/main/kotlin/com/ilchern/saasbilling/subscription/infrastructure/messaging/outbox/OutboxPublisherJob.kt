package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

interface OutboxPublisherJob {

  fun publishUnpublishedMessages()
}
