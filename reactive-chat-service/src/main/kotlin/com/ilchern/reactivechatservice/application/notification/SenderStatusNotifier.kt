package com.ilchern.reactivechatservice.application.notification

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionEmitService
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class SenderStatusNotifier(
  override val eventType: ChatEventType,
  private val chatEventCodec: ChatEventCodec,
  private val chatEventFactory: ChatEventFactory,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
) : Notifier {

  override fun notify(event: ChatEventEnvelope): Mono<Long> {
    val statusEnvelope = chatEventFactory.messageStatus(event, eventType)
    val payload = chatEventCodec.encode(statusEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId == event.senderId }
      .next()
      .flatMap { session -> sessionEmitService.emit(session, payload).thenReturn(1L) }
      .defaultIfEmpty(0L)
  }
}
