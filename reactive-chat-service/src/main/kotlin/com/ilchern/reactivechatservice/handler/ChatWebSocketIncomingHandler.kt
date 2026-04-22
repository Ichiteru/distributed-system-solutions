package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.application.message.ChatMessageService
import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.redis.RedisChatEventPublisher
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import com.ilchern.reactivechatservice.model.dto.ChatMessagePayload
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.UUID

@Component
class ChatWebSocketIncomingHandler(
  private val chatEventCodec: ChatEventCodec,
  private val errorHandler: ChatWebSocketErrorHandler,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val rateLimitedChatMessageService: ChatMessageService,
) {

  fun handle(
    session: WebSocketSession,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return session.receive()
      .flatMap { message -> handleMessage(message, context, outboundSink) }
      .then()
  }

  private fun handleMessage(
    message: WebSocketMessage,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return Mono.fromCallable { chatEventCodec.decode(message.payloadAsText) }
      .flatMap { envelope -> handleDecodedMessage(envelope, context, outboundSink) }
      .onErrorResume { throwable -> errorHandler.incomingDecodeFailure(context, outboundSink, throwable) }
  }

  private fun handleDecodedMessage(
    envelope: IncomingChatSocketMessage,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return handle(context.chatId, context.userId, envelope)
      .onErrorResume { throwable ->
        errorHandler.incomingProcessingFailure(
          context = context,
          outboundSink = outboundSink,
          correlationId = envelope.correlationId,
          throwable = throwable,
        )
      }
  }

  private fun handle(chatId: String, senderId: String, incoming: IncomingChatSocketMessage): Mono<Void> {
    val normalizedSocketMessage = normalize(incoming)

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
        rateLimitedChatMessageService.createMessage(envelope)
          .then()

      ChatEventType.CHAT_TYPING_STARTED, ChatEventType.CHAT_TYPING_STOPPED ->
        redisChatEventPublisher.publishCreated(envelope).then()

      else -> Mono.empty()
    }
  }

  private fun normalize(socketMessage: IncomingChatSocketMessage): IncomingChatSocketMessage {
    return socketMessage.copy(
      eventId = socketMessage.eventId.ifBlank { UUID.randomUUID().toString() },
      correlationId = socketMessage.correlationId ?: socketMessage.eventId.ifBlank { UUID.randomUUID().toString() },
      timestamp = socketMessage.timestamp,
    )
  }
}
