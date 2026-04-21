package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.application.notification.NotifierRegistry
import com.ilchern.reactivechatservice.application.ratelimit.RateLimiterService
import com.ilchern.reactivechatservice.infrastructure.metrics.ChatMessageMetrics
import com.ilchern.reactivechatservice.infrastructure.redis.RedisChatEventPublisher
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface ChatMessageService {
  fun handleIncoming(chatId: String, senderId: String, envelope: ChatEventEnvelope): Mono<Void>
}

@Service
class DefaultChatMessageService(
  private val inboundChatEventNormalizer: InboundChatEventNormalizer,
  private val chatMessageWriter: ChatMessageWriter,
  private val rateLimitRejectionNotifier: RateLimitRejectionNotifier,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val rateLimiterService: RateLimiterService,
  private val chatMessageMetrics: ChatMessageMetrics,
  private val notifierRegistry: NotifierRegistry,
  private val chatEventFactory: ChatEventFactory,
) : ChatMessageService {

  override fun handleIncoming(chatId: String, senderId: String, envelope: ChatEventEnvelope): Mono<Void> {
    val normalizedEnvelope = inboundChatEventNormalizer.normalize(chatId, senderId, envelope)

    return when (normalizedEnvelope.eventType) {
      ChatEventType.CHAT_MESSAGE_CREATED -> createMessage(normalizedEnvelope).then()
      ChatEventType.CHAT_TYPING_STARTED, ChatEventType.CHAT_TYPING_STOPPED ->
        redisChatEventPublisher.publishCreated(normalizedEnvelope).then()

      else -> Mono.empty()
    }
  }

  private fun createMessage(envelope: ChatEventEnvelope): Mono<ChatMessage> {
    return rateLimiterService.tryConsume(envelope.senderId)
      .flatMap { decision ->
        if (decision.allowed) {
          chatMessageWriter.save(envelope)
            .doOnNext { chatMessageMetrics.recordAccepted() }
            .delayUntil { chatMessage ->
              val createdEnvelope = chatEventFactory.messageCreated(chatMessage)
              notifierRegistry.get(ChatEventType.CHAT_MESSAGE_ACCEPTED).notify(createdEnvelope)
                .then(redisChatEventPublisher.publishCreated(createdEnvelope))
            }
        } else {
          chatMessageMetrics.recordRejected()
          rateLimitRejectionNotifier.notifySender(envelope)
            .then(Mono.empty())
        }
      }
  }
}
