package com.ilchern.reactivechatservice.infrastructure.redis

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.event.WireChatEventEnvelope
import com.ilchern.reactivechatservice.infrastructure.metrics.RedisPubSubMetrics
import com.ilchern.reactivechatservice.model.domain.Channels
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface RedisChatEventPublisher {
  fun publishCreated(chatEventEnvelope: ChatEventEnvelope): Mono<Long>
  fun publishAccepted(chatEventEnvelope: ChatEventEnvelope): Mono<Long>
  fun publishDelivered(chatEventEnvelope: ChatEventEnvelope): Mono<Long>
  fun publishRejected(chatEventEnvelope: ChatEventEnvelope): Mono<Long>
}

@Service
class DefaultRedisChatEventPublisher(
  private val redisTemplate: ReactiveRedisTemplate<String, WireChatEventEnvelope>,
  private val chatEventCodec: ChatEventCodec,
  private val redisPubSubMetrics: RedisPubSubMetrics,
) : RedisChatEventPublisher {

  override fun publishCreated(chatEventEnvelope: ChatEventEnvelope): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_CREATED, chatEventEnvelope)
  }

  override fun publishAccepted(chatEventEnvelope: ChatEventEnvelope): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_ACCEPTED, chatEventEnvelope)
  }

  override fun publishDelivered(chatEventEnvelope: ChatEventEnvelope): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_DELIVERED, chatEventEnvelope)
  }

  override fun publishRejected(chatEventEnvelope: ChatEventEnvelope): Mono<Long> {
    return publish(Channels.CHAT_MESSAGE_REJECTED, chatEventEnvelope)
  }

  private fun publish(channel: String, event: ChatEventEnvelope): Mono<Long> {
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
