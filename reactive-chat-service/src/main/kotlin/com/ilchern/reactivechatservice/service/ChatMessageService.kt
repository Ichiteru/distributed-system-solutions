package com.ilchern.reactivechatservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatErrorPayload
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import com.ilchern.reactivechatservice.service.notifier.NotifierRegistry
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

interface ChatMessageService {
  fun handleIncoming(chatId: String, senderId: String, envelope: ChatEventEnvelope): Mono<Void>
}

@Service
class DefaultChatMessageService(
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
  private val rateLimiterService: RateLimiterService,
  private val chatMetrics: ChatMetrics,
  private val objectMapper: ObjectMapper,
  private val notifierRegistry: NotifierRegistry,
) : ChatMessageService {

  override fun handleIncoming(chatId: String, senderId: String, envelope: ChatEventEnvelope): Mono<Void> {
    val normalizedEnvelope = normalizeInboundEnvelope(chatId, senderId, envelope)

    return when (normalizedEnvelope.eventType) {
      ChatEventType.CHAT_MESSAGE_CREATED -> createMessage(normalizedEnvelope).then()
      ChatEventType.CHAT_TYPING_STARTED, ChatEventType.CHAT_TYPING_STOPPED ->
        redisChatEventPublisher.publishCreated(normalizedEnvelope).then()

      else -> Mono.empty()
    }
  }

  private fun createMessage(envelope: ChatEventEnvelope): Mono<ChatMessage> {
    val request = objectMapper.treeToValue(envelope.payload, ChatMessagePayload::class.java)

    return rateLimiterService.tryConsume(envelope.senderId)
      .flatMap { decision ->
        if (decision.allowed) {
          chatMessageRepository.save(
            ChatMessage(
              chatId = envelope.chatId,
              senderId = envelope.senderId,
              correlationId = envelope.correlationId ?: envelope.eventId,
              payload = Payload(
                type = request.type,
                value = request.value,
                createdAt = LocalDateTime.now()
              )
            )
          )
            .doOnNext { chatMetrics.recordMessageAccepted() }
            .delayUntil { chatMessage ->
              val createdEnvelope = buildMessageCreatedEnvelope(chatMessage)
              notifierRegistry.get(ChatEventType.CHAT_MESSAGE_ACCEPTED).notify(createdEnvelope)
                .then(redisChatEventPublisher.publishCreated(createdEnvelope))
            }
        } else {
          chatMetrics.recordMessageRejected()
          notifyAboutRateLimitRejection(envelope)
            .then(Mono.empty())
        }
      }
  }

  private fun normalizeInboundEnvelope(
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

  private fun notifyAboutRateLimitRejection(envelope: ChatEventEnvelope): Mono<Int> {
    val errorEnvelope = ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.ERROR,
      correlationId = envelope.correlationId,
      chatId = envelope.chatId,
      senderId = envelope.senderId,
      timestamp = Instant.now(),
      payload = objectMapper.valueToTree(
        ChatErrorPayload(
          code = "TOO_MANY_MESSAGES",
          httpStatus = 429,
          message = "Message rejected by backpressure policy",
        )
      ),
    )
    val payload = objectMapper.writeValueAsString(errorEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(envelope.chatId))
      .filter { session -> session.userId == envelope.senderId }
      .next()
      .flatMap { session -> sessionEmitService.emit(session, payload).thenReturn(1) }
      .defaultIfEmpty(0)
  }

  private fun buildMessageCreatedEnvelope(chatMessage: ChatMessage): ChatEventEnvelope {
    return ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.CHAT_MESSAGE_CREATED,
      correlationId = chatMessage.correlationId,
      chatId = chatMessage.chatId,
      senderId = chatMessage.senderId,
      timestamp = chatMessage.payload.createdAt.atZone(java.time.ZoneOffset.UTC).toInstant(),
      payload = objectMapper.valueToTree(
        ChatMessagePayload(
          type = chatMessage.payload.type,
          value = chatMessage.payload.value,
          messageId = chatMessage.id,
        )
      ),
    )
  }
}
