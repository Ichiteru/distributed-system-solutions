package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.application.event.ChatEventCodec
import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionEmitService
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class RateLimitRejectionNotifier(
  private val chatEventFactory: ChatEventFactory,
  private val chatEventCodec: ChatEventCodec,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
) {

  fun notifySender(envelope: ChatEventEnvelope): Mono<Int> {
    val errorEnvelope = chatEventFactory.error(
      event = envelope,
      code = TOO_MANY_MESSAGES_CODE,
      httpStatus = TOO_MANY_MESSAGES_HTTP_STATUS,
      message = TOO_MANY_MESSAGES_MESSAGE,
    )
    val payload = chatEventCodec.encode(errorEnvelope)

    return Flux.fromIterable(registry.getSessionsByChatId(envelope.chatId))
      .filter { session -> session.userId == envelope.senderId }
      .next()
      .flatMap { session -> sessionEmitService.emit(session, payload).thenReturn(1) }
      .defaultIfEmpty(0)
  }

  private companion object {
    private const val TOO_MANY_MESSAGES_CODE = "TOO_MANY_MESSAGES"
    private const val TOO_MANY_MESSAGES_HTTP_STATUS = 429
    private const val TOO_MANY_MESSAGES_MESSAGE = "Message rejected by backpressure policy"
  }
}
