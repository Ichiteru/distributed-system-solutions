package com.ilchern.reactivechatservice.config

import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.application.notification.Notifier
import com.ilchern.reactivechatservice.application.notification.SenderNotifier
import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionOutboundDispatcher
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SenderNotifierConfig {

  @Bean
  fun acceptedNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionOutboundDispatcher: SessionOutboundDispatcher,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_ACCEPTED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionOutboundDispatcher = sessionOutboundDispatcher,
    )
  }

  @Bean
  fun deliveredNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionOutboundDispatcher: SessionOutboundDispatcher,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_DELIVERED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionOutboundDispatcher = sessionOutboundDispatcher,
    )
  }

  @Bean
  fun rejectedNotifier(
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionOutboundDispatcher: SessionOutboundDispatcher,
  ): Notifier {
    return senderStatusNotifier(
      eventType = ChatEventType.CHAT_MESSAGE_REJECTED,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionOutboundDispatcher = sessionOutboundDispatcher,
    )
  }

  private fun senderStatusNotifier(
    eventType: ChatEventType,
    chatEventCodec: ChatEventCodec,
    chatEventFactory: ChatEventFactory,
    registry: SessionRegistry,
    sessionOutboundDispatcher: SessionOutboundDispatcher,
  ): SenderNotifier {
    return SenderNotifier(
      eventType = eventType,
      chatEventCodec = chatEventCodec,
      chatEventFactory = chatEventFactory,
      registry = registry,
      sessionOutboundDispatcher = sessionOutboundDispatcher,
    )
  }
}
