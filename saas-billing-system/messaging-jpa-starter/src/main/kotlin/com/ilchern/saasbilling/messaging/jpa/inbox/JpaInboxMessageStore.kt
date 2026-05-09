package com.ilchern.saasbilling.messaging.jpa.inbox

import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageStore
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException

class JpaInboxMessageStore(
  private val inboxMessageJpaRepository: InboxMessageJpaRepository,
) : InboxMessageStore {

  override fun saveIfAbsent(message: InboxMessage): Boolean =
    try {
      inboxMessageJpaRepository.save(
        InboxMessageEntity(
          id = UUID.randomUUID(),
          consumer = message.consumer,
          messageId = message.messageId,
          messageType = message.messageType,
          aggregateId = message.aggregateId,
          correlationId = message.correlationId,
          causationId = message.causationId,
          receivedAt = message.receivedAt,
          payload = message.payload,
          headers = message.headers,
        ),
      )
      true
    } catch (_: DataIntegrityViolationException) {
      false
    }
}
