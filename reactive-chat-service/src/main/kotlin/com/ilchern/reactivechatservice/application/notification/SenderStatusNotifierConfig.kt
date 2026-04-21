package com.ilchern.reactivechatservice.application.notification

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionEmitService
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SenderStatusNotifierConfig {

  @Bean
  fun acceptedNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionEmitService: SessionEmitService,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_ACCEPTED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionEmitService = sessionEmitService,
    )
  }

  @Bean
  fun deliveredNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionEmitService: SessionEmitService,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_DELIVERED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionEmitService = sessionEmitService,
    )
  }

  @Bean
  fun rejectedNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionEmitService: SessionEmitService,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_REJECTED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionEmitService = sessionEmitService,
    )
  }

  private fun senderStatusNotifier(
    eventType: ChatEventType,
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionEmitService: SessionEmitService,
  ): SenderStatusNotifier {
    return SenderStatusNotifier(
      eventType = eventType,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionEmitService = sessionEmitService,
    )
  }
}
