package com.ilchern.saasbilling.messaging.jpa.inbox

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface InboxMessageJpaRepository : JpaRepository<InboxMessageEntity, UUID> {

  fun existsByConsumerAndMessageId(
    consumer: String,
    messageId: UUID,
  ): Boolean
}
