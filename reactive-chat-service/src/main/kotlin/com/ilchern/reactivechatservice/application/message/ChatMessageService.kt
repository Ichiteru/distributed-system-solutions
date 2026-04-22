package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.application.notification.NotifierRegistry
import com.ilchern.reactivechatservice.application.ratelimit.RateLimitRejectionNotifier
import com.ilchern.reactivechatservice.application.ratelimit.RateLimiterService
import com.ilchern.reactivechatservice.infrastructure.metrics.ChatMessageMetrics
import com.ilchern.reactivechatservice.infrastructure.persistence.mongo.ChatMessageRepository
import com.ilchern.reactivechatservice.infrastructure.redis.RedisChatEventPublisher
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.dto.ChatMessagePayload
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface ChatMessageService {
  fun createMessage(envelope: ChatEvent): Mono<ChatMessage>
}

@Service
class DefaultChatMessageService(
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val chatMessageMetrics: ChatMessageMetrics,
  private val notifierRegistry: NotifierRegistry,
  private val chatEventFactory: ChatEventFactory,
  private val chatMessageRepository: ChatMessageRepository,
) : ChatMessageService {

  override fun createMessage(envelope: ChatEvent): Mono<ChatMessage> {
    val request = requireNotNull(envelope.payload as? ChatMessagePayload) {
      "Expected ${ChatMessagePayload::class.simpleName} for ${envelope.eventType}"
    }
    return chatMessageRepository.save(
      ChatMessage(
        chatId = envelope.chatId,
        senderId = envelope.senderId,
        correlationId = envelope.correlationId ?: envelope.eventId,
        payload = Payload(
          type = request.type,
          value = request.value,
          createdAt = LocalDateTime.now(),
        ),
      )
    )
      .doOnNext { chatMessageMetrics.recordAccepted() }
      .delayUntil { chatMessage ->
        val createdEnvelope = chatEventFactory.messageCreated(chatMessage)
        notifierRegistry.get(ChatEventType.CHAT_MESSAGE_ACCEPTED).notify(createdEnvelope)
          .then(redisChatEventPublisher.publishCreated(createdEnvelope))
      }
  }
}

@Service
class RateLimitedChatMessageService(
  private val defaultChatMessageService: ChatMessageService,
  private val rateLimitRejectionNotifier: RateLimitRejectionNotifier,
  private val rateLimiterService: RateLimiterService,
  private val chatMessageMetrics: ChatMessageMetrics,
) : ChatMessageService {

  override fun createMessage(envelope: ChatEvent): Mono<ChatMessage> {
    return rateLimiterService.tryConsume(envelope.senderId)
      .flatMap { decision ->
        if (decision.allowed) {
          defaultChatMessageService.createMessage(envelope)
        } else {
          chatMessageMetrics.recordRejected()
          rateLimitRejectionNotifier.notifySender(envelope)
            .then(Mono.empty())
        }
      }
  }
}