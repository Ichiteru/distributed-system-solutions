package com.ilchern.saasbilling.billing.infrastructure.messaging.inbox

import com.ilchern.saasbilling.billing.application.port.InboxMessage
import com.ilchern.saasbilling.billing.application.port.InboxMessageStore
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
