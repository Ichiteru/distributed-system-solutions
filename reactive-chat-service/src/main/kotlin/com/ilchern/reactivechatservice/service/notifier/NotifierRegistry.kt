package com.ilchern.reactivechatservice.service.notifier

import com.ilchern.reactivechatservice.model.api.ChatEventType
import org.springframework.stereotype.Service

@Service
class NotifierRegistry(
  notifiers: List<Notifier>
) {

  private val notifierMap = notifiers.associateBy { it.eventType }

  fun get(eventType: ChatEventType) : Notifier {
    return notifierMap[eventType] ?: error("No registered notifier by $eventType")
  }
}
