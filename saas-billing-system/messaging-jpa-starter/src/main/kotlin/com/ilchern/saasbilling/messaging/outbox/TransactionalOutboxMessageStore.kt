package com.ilchern.saasbilling.messaging.outbox

interface TransactionalOutboxMessageStore {

  fun append(messages: List<OutboxMessage>)
}
