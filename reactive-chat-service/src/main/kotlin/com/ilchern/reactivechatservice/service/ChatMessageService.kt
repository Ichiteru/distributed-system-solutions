package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.application.message.ChatMessageWriter
import com.ilchern.reactivechatservice.application.message.InboundChatEventNormalizer
import com.ilchern.reactivechatservice.application.message.RateLimitRejectionNotifier
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.service.notifier.NotifierRegistry
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
  private val chatMetrics: ChatMetrics,
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
            .doOnNext { chatMetrics.recordMessageAccepted() }
            .delayUntil { chatMessage ->
              val createdEnvelope = chatEventFactory.messageCreated(chatMessage)
              notifierRegistry.get(ChatEventType.CHAT_MESSAGE_ACCEPTED).notify(createdEnvelope)
                .then(redisChatEventPublisher.publishCreated(createdEnvelope))
            }
        } else {
          chatMetrics.recordMessageRejected()
          rateLimitRejectionNotifier.notifySender(envelope)
            .then(Mono.empty())
        }
      }
  }
}
