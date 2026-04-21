package com.ilchern.reactivechatservice.service.notifier

import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import reactor.core.publisher.Mono

interface Notifier {

  val eventType: ChatEventType

  fun notify(event: ChatEventEnvelope): Mono<Long>

  fun extractMessageId(event: ChatEventEnvelope): String? {
    return event.payload
      ?.get("messageId")
      ?.takeIf { !it.isNull }
      ?.asText()
  }
}
