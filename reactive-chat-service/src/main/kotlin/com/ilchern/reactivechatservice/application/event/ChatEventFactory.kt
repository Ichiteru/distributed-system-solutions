package com.ilchern.reactivechatservice.application.event

import com.ilchern.reactivechatservice.model.dto.ChatErrorPayload
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import com.ilchern.reactivechatservice.model.dto.ChatMessagePayload
import com.ilchern.reactivechatservice.model.dto.ChatMessageStatusPayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Component
class ChatEventFactory {

  fun messageCreated(chatMessage: ChatMessage): ChatEvent {
    return ChatEvent(
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
    event: ChatEvent,
    code: String,
    httpStatus: Int,
    message: String,
  ): ChatEvent {
    return ChatEvent(
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
    event: ChatEvent,
    eventType: ChatEventType,
  ): ChatEvent {
    require(eventType in SENDER_STATUS_EVENT_TYPES) { "Unsupported sender status event type: $eventType" }

    return ChatEvent(
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

  private fun extractMessageId(event: ChatEvent): String? {
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
