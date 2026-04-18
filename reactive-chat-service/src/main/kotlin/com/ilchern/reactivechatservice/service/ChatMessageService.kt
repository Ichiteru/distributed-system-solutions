package com.ilchern.reactivechatservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatMessageCallback
import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.ChatMessageState
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent
import com.ilchern.reactivechatservice.model.event.PayloadEvent
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.LocalDateTime

interface ChatMessageService {
  fun create(chatId: String, senderId: String, request: ChatMessageRequest): Mono<ChatMessage>
  fun sendMessageToReceiver(event: ChatMessageEvent): Mono<Long>
  fun notifyAboutDelivery(event: ChatMessageEvent): Mono<Int>
  fun notifyAboutRejection(event: ChatMessageEvent): Mono<Int>
  fun notifyAboutAcceptance(event: ChatMessageEvent): Mono<Int>
}

@Service
class DefaultChatMessageService(
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
  private val objectMapper: ObjectMapper,
) : ChatMessageService {

  override fun create(chatId: String, senderId: String, request: ChatMessageRequest): Mono<ChatMessage> {
    return chatMessageRepository.save(
      ChatMessage(
        chatId = chatId,
        senderId = senderId,
        correlationId = request.correlationId,
        payload = Payload(
          type = request.type,
          value = request.value,
          createdAt = LocalDateTime.now()
        )
      )
    )
      .delayUntil { chatMessage ->
        val chatMessageEvent = ChatMessageEvent(
          id = chatMessage.id,
          correlationId = chatMessage.correlationId,
          chatId = chatMessage.chatId,
          senderId = chatMessage.senderId,
          payload = PayloadEvent(
            type = chatMessage.payload.type,
            value = chatMessage.payload.value,
            createdAt = chatMessage.payload.createdAt
          )
        )
        notifyAboutAcceptance(chatMessageEvent)
          .then(redisChatEventPublisher.publishCreated(chatMessageEvent))
      }
  }

  override fun sendMessageToReceiver(event: ChatMessageEvent): Mono<Long> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageRequest(
        type = event.payload.type,
        value = event.payload.value,
        correlationId = event.correlationId
      )
    )
    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId != event.senderId }
      .flatMap { session ->
        sessionEmitService.emit(session, payload)
          .flatMap { emitResult ->
            when (emitResult) {
              Sinks.EmitResult.OK -> redisChatEventPublisher.publishDelivered(event)

              Sinks.EmitResult.FAIL_TERMINATED, Sinks.EmitResult.FAIL_CANCELLED ->
                Mono.fromSupplier { registry.remove(session.sessionId) }
                  .thenReturn(0)

              else -> redisChatEventPublisher.publishRejected(event)

            }
          }
      }
      .reduce(0L) { acc, next -> acc.plus(next) }
  }

  override fun notifyAboutDelivery(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageCallback(
        correlationId = event.correlationId,
        state = ChatMessageState.DELIVERED
      )
    )

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session ->
        sessionEmitService.emit(session, payload)
          .thenReturn(1)
      }
      .defaultIfEmpty(0)
  }

  override fun notifyAboutRejection(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageCallback(
        correlationId = event.correlationId,
        state = ChatMessageState.REJECTED
      )
    )

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session -> sessionEmitService.emit(session, payload).thenReturn(1) }
      .defaultIfEmpty(0)
  }

  override fun notifyAboutAcceptance(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageCallback(
        correlationId = event.correlationId,
        state = ChatMessageState.ACCEPTED
      )
    )

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session ->
        sessionEmitService.emit(session, payload).thenReturn(1)
      }
      .defaultIfEmpty(0)
  }
}
