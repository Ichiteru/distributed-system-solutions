package com.ilchern.reactivechatservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.domain.EventTypes
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface RedisChatEventPublisher {
  fun publishCreated(event: Any): Mono<Long>
  fun publishDelivered(event: Any): Mono<Long>
  fun publishRejected(event: Any): Mono<Long>
}

@Service
class DefaultRedisChatEventPublisher(
  private val redisTemplate: ReactiveRedisTemplate<String, String>,
  private val objectMapper: ObjectMapper,
) : RedisChatEventPublisher {

  override fun publishCreated(event: Any): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_CREATED, event)
  }

  override fun publishDelivered(event: Any): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_DELIVERED, event)
  }

  override fun publishRejected(event: Any): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_REJECTED, event)
  }

  private fun publish(channel: String, event: Any): Mono<Long> {
    return Mono.fromCallable { objectMapper.writeValueAsString(event) }
      .flatMap { payload -> redisTemplate.convertAndSend(channel, payload) }
      .doOnNext { receivers ->
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
