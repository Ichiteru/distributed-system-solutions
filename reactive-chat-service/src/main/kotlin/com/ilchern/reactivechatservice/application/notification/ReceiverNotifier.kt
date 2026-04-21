package com.ilchern.reactivechatservice.application.notification

import com.ilchern.reactivechatservice.application.event.ChatEventCodec
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.infrastructure.metrics.ChatMessageMetrics
import com.ilchern.reactivechatservice.infrastructure.metrics.DeliveryMetrics
import com.ilchern.reactivechatservice.infrastructure.redis.RedisChatEventPublisher
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionEmitService
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Service
class ReceiverNotifier(
  override val eventType: ChatEventType = ChatEventType.CHAT_MESSAGE_CREATED,
  private val chatEventCodec: ChatEventCodec,
  private val registry: SessionRegistry,
  private val sessionEmitService: SessionEmitService,
  private val chatMessageMetrics: ChatMessageMetrics,
  private val deliveryMetrics: DeliveryMetrics,
  private val redisChatEventPublisher: RedisChatEventPublisher,
) : Notifier {

  override fun notify(event: ChatEventEnvelope): Mono<Long> {
    val payload = chatEventCodec.encode(event)
    val priority = priorityOf(event)

    return Flux.fromIterable(registry.getSessionsByChatId(event.chatId))
      .filter { session -> session.userId != event.senderId }
      .flatMap { session ->
        sessionEmitService.emit(session, payload, priority)
          .flatMap { emitResult ->
            when (emitResult) {
              Sinks.EmitResult.OK -> {
                if (priority == OutboundMessagePriority.CRITICAL) {
                  deliveryMetrics.recordDeliveryLatency(event.timestamp)
                  redisChatEventPublisher.publishDelivered(event)
                } else {
                  Mono.just(0L)
                }
              }

              Sinks.EmitResult.FAIL_TERMINATED, Sinks.EmitResult.FAIL_CANCELLED ->
                Mono.fromSupplier { registry.remove(session.sessionId) }
                  .thenReturn(0L)

              else -> {
                if (priority == OutboundMessagePriority.CRITICAL) {
                  chatMessageMetrics.recordRejected()
                  redisChatEventPublisher.publishRejected(event)
                } else {
                  Mono.just(0L)
                }
              }
            }
          }
      }
      .reduce(0L) { acc, next -> acc.plus(next) }
  }

  private fun priorityOf(event: ChatEventEnvelope): OutboundMessagePriority {
    return when (event.eventType) {
      ChatEventType.CHAT_TYPING_STARTED,
      ChatEventType.CHAT_TYPING_STOPPED -> OutboundMessagePriority.EPHEMERAL

      else -> OutboundMessagePriority.CRITICAL
    }
  }
}
