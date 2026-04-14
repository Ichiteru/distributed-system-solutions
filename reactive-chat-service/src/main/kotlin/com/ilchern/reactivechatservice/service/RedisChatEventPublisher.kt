package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.model.domain.EventTypes
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface RedisChatEventPublisher {
  fun publishCreated(chatMessageEvent: ChatMessageEvent): Mono<Long>
  fun publishDelivered(chatMessageEvent: ChatMessageEvent): Mono<Long>
  fun publishRejected(chatMessageEvent: ChatMessageEvent): Mono<Long>
}

@Service
class DefaultRedisChatEventPublisher(
  private val redisTemplate: ReactiveRedisTemplate<String, ChatMessageEvent>,
) : RedisChatEventPublisher {

  override fun publishCreated(chatMessageEvent: ChatMessageEvent): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_CREATED, chatMessageEvent)
  }

  override fun publishDelivered(chatMessageEvent: ChatMessageEvent): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_DELIVERED, chatMessageEvent)
  }

  override fun publishRejected(chatMessageEvent: ChatMessageEvent): Mono<Long> {
    return publish(EventTypes.CHAT_MESSAGE_REJECTED, chatMessageEvent)
  }

  private fun publish(channel: String, event: ChatMessageEvent): Mono<Long> {
    return redisTemplate.convertAndSend(channel, event)
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
