package com.ilchern.saasbilling.subscription.infrastructure.messaging.inbox

import com.ilchern.saasbilling.subscription.application.port.InboxMessage
import com.ilchern.saasbilling.subscription.application.port.InboxMessageStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InboxMessageProcessor(
  private val inboxMessageStore: InboxMessageStore,
) {

  @Transactional
  fun process(
    message: InboxMessage,
    action: () -> Unit,
  ): Boolean {
    if (!inboxMessageStore.saveIfAbsent(message)) {
      return false
    }
    action()
    return true
  }
}
