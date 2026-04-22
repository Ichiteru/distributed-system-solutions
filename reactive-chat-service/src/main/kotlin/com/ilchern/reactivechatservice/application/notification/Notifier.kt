package com.ilchern.reactivechatservice.application.notification

import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import reactor.core.publisher.Mono

interface Notifier {

  val eventType: ChatEventType

  fun notify(event: ChatEvent): Mono<Long>
}
