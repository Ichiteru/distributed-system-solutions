package com.ilchern.reactivechatservice.infrastructure.redis

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.event.ChannelChatEvent
import com.ilchern.reactivechatservice.infrastructure.metrics.RedisPubSubMetrics
import com.ilchern.reactivechatservice.model.domain.Channels
import com.ilchern.reactivechatservice.model.api.ChatEvent
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface RedisChatEventPublisher {
  fun publishCreated(chatEvent: ChatEvent): Mono<Long>
  fun publishAccepted(chatEvent: ChatEvent): Mono<Long>
  fun publishDelivered(chatEvent: ChatEvent): Mono<Long>
  fun publishRejected(chatEvent: ChatEvent): Mono<Long>
}

@Service
class DefaultRedisChatEventPublisher(
  private val redisTemplate: ReactiveRedisTemplate<String, ChannelChatEvent>,
  private val chatEventCodec: ChatEventCodec,
  private val redisPubSubMetrics: RedisPubSubMetrics,
) : RedisChatEventPublisher {

  override fun publishCreated(chatEvent: ChatEvent): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_CREATED, chatEvent)
  }

  override fun publishAccepted(chatEvent: ChatEvent): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_ACCEPTED, chatEvent)
  }

  override fun publishDelivered(chatEvent: ChatEvent): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_DELIVERED, chatEvent)
  }

  override fun publishRejected(chatEvent: ChatEvent): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_REJECTED, chatEvent)
  }

  private fun publish(channel: String, event: ChatEvent): Mono<Long> {
    return redisTemplate.convertAndSend(channel, chatEventCodec.toWire(event))
      .doOnNext { receivers ->
        redisPubSubMetrics.recordPublished(channel)
        log.info("Redis published event: channel={}, receivers={}", channel, receivers)
      }
      .doOnError { error ->
        log.error("Redis publish failed: channel={}", channel, error)
      }
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}
