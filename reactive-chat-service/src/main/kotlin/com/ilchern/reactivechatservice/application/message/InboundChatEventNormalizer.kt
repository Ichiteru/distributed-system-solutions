package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InboundChatEventNormalizer {

  fun normalize(
    chatId: String,
    senderId: String,
    envelope: ChatEventEnvelope,
  ): ChatEventEnvelope {
    return envelope.copy(
      eventId = envelope.eventId.ifBlank { UUID.randomUUID().toString() },
      correlationId = envelope.correlationId ?: envelope.eventId.ifBlank { UUID.randomUUID().toString() },
      chatId = chatId,
      senderId = senderId,
      timestamp = envelope.timestamp,
    )
  }
}
