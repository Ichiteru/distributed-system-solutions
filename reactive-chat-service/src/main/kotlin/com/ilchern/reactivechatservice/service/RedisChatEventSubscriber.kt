package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.model.domain.EventTypes
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import reactor.core.Disposable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@Service
class RedisChatEventSubscriber(
  private val listenerContainer: ReactiveRedisMessageListenerContainer,
) {

  private var subscription: Disposable? = null

  @PostConstruct
  fun subscribe() {
    subscription = listenerContainer.receive(
      ChannelTopic.of(EventTypes.CHAT_MESSAGE_CREATED),
      ChannelTopic.of(EventTypes.CHAT_MESSAGE_DELIVERED),
      ChannelTopic.of(EventTypes.CHAT_MESSAGE_REJECTED),
    )
      .doOnNext { message ->
        val channel = message.channel
        val payload = message.message
        log.info("Redis consumed event: channel={}, payload={}", channel, payload)
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
