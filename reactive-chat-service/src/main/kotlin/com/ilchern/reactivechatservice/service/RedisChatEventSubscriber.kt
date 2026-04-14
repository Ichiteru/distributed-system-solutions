package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.model.domain.EventTypes
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.Disposable
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent

@Service
class RedisChatEventSubscriber(
  private val redisTemplate: ReactiveRedisTemplate<String, ChatMessageEvent>,
  private val sessionRegistry: SessionRegistry,
  private val chatMessageService: ChatMessageService,
) {

  private var subscription: Disposable? = null

  @PostConstruct
  fun subscribe() {
    subscription = redisTemplate.listenToChannel(
      EventTypes.CHAT_MESSAGE_CREATED,
      EventTypes.CHAT_MESSAGE_DELIVERED,
      EventTypes.CHAT_MESSAGE_REJECTED,
    )
      .flatMap { message -> chatMessageService.sendMessageToChat(message.message) }
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
