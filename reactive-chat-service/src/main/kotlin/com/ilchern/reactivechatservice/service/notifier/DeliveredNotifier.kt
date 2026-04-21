package com.ilchern.reactivechatservice.service.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.service.SessionEmitService
import com.ilchern.reactivechatservice.service.SessionRegistry
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class DeliveredNotifier(
  override val eventType: ChatEventType = ChatEventType.CHAT_MESSAGE_DELIVERED,
  private val objectMapper: ObjectMapper,
  private val chatEventFactory: ChatEventFactory,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
) : Notifier {

  override fun notify(event: ChatEventEnvelope): Mono<Long> {
    val deliveryEnvelope = chatEventFactory.delivered(event)
    val payload = objectMapper.writeValueAsString(deliveryEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session ->
        sessionEmitService.emit(session, payload)
          .thenReturn(1L)
      }
      .defaultIfEmpty(0L)
  }
}
