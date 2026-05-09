package com.ilchern.saasbilling.messaging.inbox

import org.springframework.transaction.annotation.Transactional

open class InboxMessageProcessor(
  private val inboxMessageStore: InboxMessageStore,
) {

  @Transactional
  open fun process(
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
