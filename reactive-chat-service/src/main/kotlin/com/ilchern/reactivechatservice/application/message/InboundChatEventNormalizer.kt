package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InboundChatEventNormalizer {

  fun normalize(socketMessage: IncomingChatSocketMessage): IncomingChatSocketMessage {
    return socketMessage.copy(
      eventId = socketMessage.eventId.ifBlank { UUID.randomUUID().toString() },
      correlationId = socketMessage.correlationId ?: socketMessage.eventId.ifBlank { UUID.randomUUID().toString() },
      timestamp = socketMessage.timestamp,
    )
  }
}
