package com.ilchern.reactivechatservice.application.event

import com.ilchern.reactivechatservice.model.api.ChatErrorPayload
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.api.ChatMessageStatusPayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Component
class ChatEventFactory {

  fun messageCreated(chatMessage: ChatMessage): ChatEventEnvelope {
    return ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.CHAT_MESSAGE_CREATED,
      correlationId = chatMessage.correlationId,
      chatId = chatMessage.chatId,
      senderId = chatMessage.senderId,
      timestamp = chatMessage.payload.createdAt.atZone(ZoneOffset.UTC).toInstant(),
      payload = ChatMessagePayload(
        type = chatMessage.payload.type,
        value = chatMessage.payload.value,
        messageId = chatMessage.id,
      ),
    )
  }

  fun error(
    event: ChatEventEnvelope,
    code: String,
    httpStatus: Int,
    message: String,
  ): ChatEventEnvelope {
    return ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.ERROR,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = Instant.now(),
      payload = ChatErrorPayload(
        code = code,
        httpStatus = httpStatus,
        message = message,
      ),
    )
  }

  fun messageStatus(
    event: ChatEventEnvelope,
    eventType: ChatEventType,
  ): ChatEventEnvelope {
    require(eventType in SENDER_STATUS_EVENT_TYPES) { "Unsupported sender status event type: $eventType" }

    return ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = eventType,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = Instant.now(),
      payload = ChatMessageStatusPayload(
        messageId = extractMessageId(event),
        status = eventType.value,
      ),
    )
  }

  private fun extractMessageId(event: ChatEventEnvelope): String? {
    return (event.payload as? ChatMessagePayload)?.messageId
  }

  private companion object {
    private val SENDER_STATUS_EVENT_TYPES = setOf(
      ChatEventType.CHAT_MESSAGE_ACCEPTED,
      ChatEventType.CHAT_MESSAGE_DELIVERED,
      ChatEventType.CHAT_MESSAGE_REJECTED,
    )
  }
}
