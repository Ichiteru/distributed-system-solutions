package com.ilchern.reactivechatservice.infrastructure.redis

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.redis.WireChatEvent
import com.ilchern.reactivechatservice.infrastructure.metrics.RedisPubSubMetrics
import com.ilchern.reactivechatservice.infrastructure.redis.Channels
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import com.ilchern.reactivechatservice.application.notification.NotifierRegistry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono

@Service
class RedisChatEventSubscriber(
  private val redisTemplate: ReactiveRedisTemplate<String, WireChatEvent>,
  private val chatEventCodec: ChatEventCodec,
  private val redisPubSubMetrics: RedisPubSubMetrics,
  private val notifierRegistry: NotifierRegistry,
) {
  private var subscription: Disposable? = null

  @PostConstruct
  fun subscribe() {
    subscription = redisTemplate.listenToChannel(
      Channels.CHAT_MESSAGE_CREATED,
      Channels.CHAT_MESSAGE_DELIVERED,
      Channels.CHAT_MESSAGE_REJECTED,
    )
      .flatMap { message ->
        redisPubSubMetrics.recordConsumed(message.channel)
        val event = chatEventCodec.fromWire(message.message)
        when (message.channel) {
          Channels.CHAT_MESSAGE_CREATED ->
            notifierRegistry.get(ChatEventType.CHAT_MESSAGE_CREATED).notify(event)

          Channels.CHAT_MESSAGE_DELIVERED ->
            notifierRegistry.get(ChatEventType.CHAT_MESSAGE_DELIVERED).notify(event)

          Channels.CHAT_MESSAGE_REJECTED ->
            notifierRegistry.get(ChatEventType.CHAT_MESSAGE_REJECTED).notify(event)

          else -> Mono.just(1L)
        }
      }
      .doOnError { error ->
        log.error("Redis subscriber failed", error)
      }
      .retry()
      .subscribe()
  }

  @PreDestroy
  fun unsubscribe() {
    subscription?.dispose()
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}
