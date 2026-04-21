package com.ilchern.reactivechatservice.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatErrorPayload
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventPayload
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.api.ChatMessageStatusPayload
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import org.springframework.stereotype.Component

@Component
class ChatEventCodec(
  private val objectMapper: ObjectMapper,
) {

  fun encode(event: ChatEventEnvelope): String {
    return objectMapper.writeValueAsString(toWire(event))
  }

  fun decode(payload: String): IncomingChatSocketMessage {
    return objectMapper.readValue(payload, IncomingChatSocketMessage::class.java)
  }

  fun toWire(event: ChatEventEnvelope): WireChatEventEnvelope {
    return WireChatEventEnvelope(
      eventId = event.eventId,
      eventType = event.eventType.value,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = event.timestamp,
      payload = event.payload?.let(objectMapper::valueToTree),
    )
  }

  fun fromWire(event: WireChatEventEnvelope): ChatEventEnvelope {
    return ChatEventEnvelope(
      eventId = event.eventId,
      eventType = ChatEventType.fromValue(event.eventType),
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = event.timestamp,
      payload = decodePayload(event),
    )
  }

  private fun decodePayload(event: WireChatEventEnvelope): ChatEventPayload? {
    val payload = event.payload ?: return null

    return when (ChatEventType.fromValue(event.eventType)) {
      ChatEventType.CHAT_MESSAGE_CREATED,
      ChatEventType.CHAT_TYPING_STARTED,
      ChatEventType.CHAT_TYPING_STOPPED -> objectMapper.treeToValue(payload, ChatMessagePayload::class.java)

      ChatEventType.CHAT_MESSAGE_ACCEPTED,
      ChatEventType.CHAT_MESSAGE_DELIVERED,
      ChatEventType.CHAT_MESSAGE_REJECTED -> objectMapper.treeToValue(payload, ChatMessageStatusPayload::class.java)

      ChatEventType.ERROR -> objectMapper.treeToValue(payload, ChatErrorPayload::class.java)
    }
  }
}
