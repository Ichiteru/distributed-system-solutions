package com.ilchern.reactivechatservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatMessageCallback
import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import com.ilchern.reactivechatservice.model.domain.ChatMessage
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

  fun sendMessageToChat(event: ChatMessageEvent): Mono<Int>

  fun notifyAboutDelivery(event: ChatMessageEvent): Mono<Int>
  fun notifyAboutRejection(event: ChatMessageEvent): Mono<Int>
}

@Service
class DefaultChatMessageService(
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val registry: SessionRegistry,
  private val objectMapper: ObjectMapper,
) : ChatMessageService {

  override fun create(chatId: String, senderId: String, request: ChatMessageRequest): Mono<ChatMessage> {
    return chatMessageRepository.save(
      ChatMessage(
        chatId = chatId,
        senderId = senderId,
        correlationId = request.correlationId, // TODO
        payload = Payload(
          type = request.type,
          value = request.value,
          createdAt = LocalDateTime.now()
        )
      )
    )
      .delayUntil { chatMessage ->
        redisChatEventPublisher.publishCreated(
          ChatMessageEvent(
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
        )
      }
  }

  override fun sendMessageToChat(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageRequest(
        type = event.payload.type,
        value = event.payload.value,
        correlationId = event.correlationId
      )
    )
    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId != event.senderId }
      .map { session -> session.outboundSink.tryEmitNext(payload) }
      .flatMap { emitResult ->
        when (emitResult) { // TODO добавить обработку на каждый emit result
          Sinks.EmitResult.OK -> redisChatEventPublisher.publishDelivered(event)
            .thenReturn(1)

          else -> redisChatEventPublisher.publishRejected(event)
            .thenReturn(0)
        }
      }
      .reduce(0) { acc, next -> acc.plus(next) }
  }

  override fun notifyAboutDelivery(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(ChatMessageCallback(correlationId = event.correlationId))

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .map { session ->
        val emitResult = session.outboundSink.tryEmitNext(payload)
        when (emitResult) {
          Sinks.EmitResult.OK -> 1
          else -> 0
        }
      }
  }

  override fun notifyAboutRejection(event: ChatMessageEvent): Mono<Int> {
    val payload = objectMapper.writeValueAsString(
      ChatMessageCallback(
        correlationId = event.correlationId,
        success = false,
        error = "error" // TODO нормальная ошибка
      )
    )

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .map { session ->
        val emitResult = session.outboundSink.tryEmitNext(payload)
        when (emitResult) {
          Sinks.EmitResult.OK -> 1
          else -> 0
        }
      }
  }
}
