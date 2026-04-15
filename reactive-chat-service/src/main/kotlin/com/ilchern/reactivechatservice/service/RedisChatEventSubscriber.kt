package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.model.domain.Channels
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono

@Service
class RedisChatEventSubscriber(
  private val redisTemplate: ReactiveRedisTemplate<String, ChatMessageEvent>,
  private val chatMessageService: ChatMessageService,
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
        when (message.channel) {
          Channels.CHAT_MESSAGE_CREATED -> chatMessageService.sendMessageToReceiver(message.message)
          Channels.CHAT_MESSAGE_DELIVERED -> chatMessageService.notifyAboutDelivery(message.message)
          Channels.CHAT_MESSAGE_REJECTED -> chatMessageService.notifyAboutRejection(message.message)
          else -> Mono.just(1)
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
