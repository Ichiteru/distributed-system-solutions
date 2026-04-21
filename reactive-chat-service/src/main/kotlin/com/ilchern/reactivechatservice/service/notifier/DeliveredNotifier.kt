package com.ilchern.reactivechatservice.service.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessageStatusPayload
import com.ilchern.reactivechatservice.service.SessionEmitService
import com.ilchern.reactivechatservice.service.SessionRegistry
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class DeliveredNotifier(
  override val eventType: ChatEventType = ChatEventType.CHAT_MESSAGE_DELIVERED,
  private val objectMapper: ObjectMapper,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
) : Notifier {

  override fun notify(event: ChatEventEnvelope): Mono<Int> {
    val deliveryEnvelope = ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = eventType,
      correlationId = event.correlationId,
      chatId = event.chatId,
      senderId = event.senderId,
      timestamp = Instant.now(),
      payload = objectMapper.valueToTree(
        ChatMessageStatusPayload(
          messageId = extractMessageId(event),
          status = eventType.value,
        )
      ),
    )
    val payload = objectMapper.writeValueAsString(deliveryEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session ->
        sessionEmitService.emit(session, payload)
          .thenReturn(1)
      }
      .defaultIfEmpty(0)
  }
}