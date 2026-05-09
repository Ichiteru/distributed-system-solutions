package com.ilchern.saasbilling.messaging.jpa.outbox

import com.ilchern.saasbilling.messaging.outbox.OutboxMessage
import com.ilchern.saasbilling.messaging.outbox.TransactionalOutboxMessageStore
import java.time.LocalDateTime
import java.time.ZoneOffset

class JpaTransactionalOutboxMessageStore(
  private val outboxMessageJpaRepository: OutboxMessageJpaRepository,
) : TransactionalOutboxMessageStore {

  override fun append(messages: List<OutboxMessage>) {
    if (messages.isEmpty()) {
      return
    }

    outboxMessageJpaRepository.saveAll(messages.map(::toEntity))
  }

  private fun toEntity(message: OutboxMessage): OutboxMessageEntity =
    OutboxMessageEntity(
      id = message.id,
      aggregateType = message.aggregateType,
      aggregateId = message.aggregateId,
      type = message.type,
      payload = message.payload,
      headers = message.headers,
      timestamp = LocalDateTime.ofInstant(message.occurredAt, ZoneOffset.UTC),
    )
}
