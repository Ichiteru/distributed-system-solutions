package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.infrastructure.redis.RedisChatEventPublisher
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import com.ilchern.reactivechatservice.model.domain.ChatMessagePayload
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface IncomingSocketMessageHandler {
  fun handle(chatId: String, senderId: String, incoming: IncomingChatSocketMessage): Mono<Void>
}

@Service
class DefaultIncomingSocketMessageHandler(
  private val inboundChatEventNormalizer: InboundChatEventNormalizer,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val rateLimitedChatMessageCreatedHandler: ChatMessageCreatedHandler,
) : IncomingSocketMessageHandler {

  override fun handle(chatId: String, senderId: String, incoming: IncomingChatSocketMessage): Mono<Void> {
    val normalizedSocketMessage = inboundChatEventNormalizer.normalize(incoming)

    val envelope = ChatEvent(
      eventId = incoming.eventId,
      eventType = incoming.eventType,
      correlationId = incoming.correlationId,
      chatId = chatId,
      senderId = senderId,
      timestamp = incoming.timestamp,
      payload = ChatMessagePayload(
        type = incoming.payload.type,
        value = incoming.payload.value,
      )
    )

    return when (normalizedSocketMessage.eventType) {
      ChatEventType.CHAT_MESSAGE_CREATED ->
        rateLimitedChatMessageCreatedHandler.createMessage(envelope)
          .then()

      ChatEventType.CHAT_TYPING_STARTED, ChatEventType.CHAT_TYPING_STOPPED ->
        redisChatEventPublisher.publishCreated(envelope).then()

      else -> Mono.empty()
    }
  }

}
