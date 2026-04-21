package com.ilchern.reactivechatservice.service.notifier

import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import reactor.core.publisher.Mono

interface Notifier {

  val eventType: ChatEventType

  fun notify(event: ChatEventEnvelope): Mono<Long>
}
