package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.config.properties.HistoryProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatErrorPayload
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.api.ChatMessageStatusPayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import com.ilchern.reactivechatservice.service.backpressure.OutboundMessagePriority
import com.ilchern.reactivechatservice.service.notifier.Notifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

interface ChatMessageService {
  fun handleIncoming(chatId: String, senderId: String, envelope: ChatEventEnvelope): Mono<Void>
  fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope>
  fun sendEventToReceivers(event: ChatEventEnvelope): Mono<Long>
   fun notifyAboutRejection(event: ChatEventEnvelope): Mono<Int>
  fun notifyAboutAcceptance(event: ChatEventEnvelope): Mono<Int>
}

@Service
class DefaultChatMessageService(
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
  private val rateLimiterService: RateLimiterService,
  private val historyProperties: HistoryProperties,
  private val chatMetrics: ChatMetrics,
  private val objectMapper: ObjectMapper,
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

  override fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope> {
    val pageRequest = PageRequest.of(
      0,
      historyProperties.reconnectLimit,
      Sort.by(Sort.Direction.DESC, "payload.createdAt"),
    )

    return chatMessageRepository.findByChatId(chatId, pageRequest)
      .collectList()
      .flatMapMany { messages ->
        Flux.fromIterable(messages.asReversed())
      }
      .map(::buildMessageCreatedEnvelope)
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
              notifyAboutAcceptance(createdEnvelope)
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

  override fun sendEventToReceivers(event: ChatEventEnvelope): Mono<Long> {
    val payload = objectMapper.writeValueAsString(event)
    val priority = priorityOf(event)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId != event.senderId }
      .flatMap { session ->
        sessionEmitService.emit(session, payload, priority)
          .flatMap { emitResult ->
            when (emitResult) {
              Sinks.EmitResult.OK -> {
                if (priority == OutboundMessagePriority.CRITICAL) {
                  chatMetrics.recordDeliveryLatency(event.timestamp)
                  redisChatEventPublisher.publishDelivered(event)
                } else {
                  Mono.just(0)
                }
              }

              Sinks.EmitResult.FAIL_TERMINATED, Sinks.EmitResult.FAIL_CANCELLED ->
                Mono.fromSupplier { registry.remove(session.sessionId) }
                  .thenReturn(0)

              else -> {
                if (priority == OutboundMessagePriority.CRITICAL) {
                  chatMetrics.recordMessageRejected()
                  redisChatEventPublisher.publishRejected(event)
                } else {
                  Mono.just(0)
                }
              }

            }
          }
      }
      .reduce(0L) { acc, next -> acc.plus(next) }
  }

  override fun notifyAboutRejection(event: ChatEventEnvelope): Mono<Int> {
    val rejectionEnvelope = ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.CHAT_MESSAGE_REJECTED,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = Instant.now(),
      payload = objectMapper.valueToTree(
        ChatMessageStatusPayload(
          messageId = extractMessageId(event),
          status = ChatEventType.CHAT_MESSAGE_REJECTED.value,
        )
      ),
    )
    val payload = objectMapper.writeValueAsString(rejectionEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session -> sessionEmitService.emit(session, payload).thenReturn(1) }
      .defaultIfEmpty(0)
  }

  override fun notifyAboutAcceptance(event: ChatEventEnvelope): Mono<Int> {
    val acceptanceEnvelope = ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.CHAT_MESSAGE_ACCEPTED,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = Instant.now(),
      payload = objectMapper.valueToTree(
        ChatMessageStatusPayload(
          messageId = extractMessageId(event),
          status = ChatEventType.CHAT_MESSAGE_ACCEPTED.value,
        )
      ),
    )
    val payload = objectMapper.writeValueAsString(acceptanceEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session ->
        sessionEmitService.emit(session, payload).thenReturn(1)
      }
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

  private fun extractMessageId(event: ChatEventEnvelope): String? {
    return event.payload
      ?.get("messageId")
      ?.takeIf { !it.isNull }
      ?.asText()
  }

  private fun priorityOf(event: ChatEventEnvelope): OutboundMessagePriority {
    return when (event.eventType) {
      ChatEventType.CHAT_TYPING_STARTED,
      ChatEventType.CHAT_TYPING_STOPPED -> OutboundMessagePriority.EPHEMERAL

      else -> OutboundMessagePriority.CRITICAL
    }
  }
}
