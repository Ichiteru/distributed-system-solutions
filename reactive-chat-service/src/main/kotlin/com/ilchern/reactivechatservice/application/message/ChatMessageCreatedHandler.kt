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

interface ChatMessageCreatedHandler{
  fun createMessage(envelope: ChatEventEnvelope): Mono<ChatMessage>
}

@Service
class DefaultChatMessageCreatedHandler (
  private val chatMessageWriter: ChatMessageWriter,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val chatMessageMetrics: ChatMessageMetrics,
  private val notifierRegistry: NotifierRegistry,
  private val chatEventFactory: ChatEventFactory,
) : ChatMessageCreatedHandler{

  override fun createMessage(envelope: ChatEventEnvelope): Mono<ChatMessage> {
    return chatMessageWriter.save(envelope)
      .doOnNext { chatMessageMetrics.recordAccepted() }
      .delayUntil { chatMessage ->
        val createdEnvelope = chatEventFactory.messageCreated(chatMessage)
        notifierRegistry.get(ChatEventType.CHAT_MESSAGE_ACCEPTED).notify(createdEnvelope)
          .then(redisChatEventPublisher.publishCreated(createdEnvelope))
      }
  }
}

@Service
class RateLimitedChatMessageCreatedHandler(
  private val defaultChatMessageCreatedHandler: ChatMessageCreatedHandler,
  private val rateLimitRejectionNotifier: RateLimitRejectionNotifier,
  private val rateLimiterService: RateLimiterService,
  private val chatMessageMetrics: ChatMessageMetrics,
) : ChatMessageCreatedHandler {

  override fun createMessage(envelope: ChatEventEnvelope): Mono<ChatMessage> {
    return rateLimiterService.tryConsume(envelope.senderId)
      .flatMap { decision ->
        if (decision.allowed) {
          defaultChatMessageCreatedHandler.createMessage(envelope)
        } else {
          chatMessageMetrics.recordRejected()
          rateLimitRejectionNotifier.notifySender(envelope)
            .then(Mono.empty())
        }
      }
  }
}