package com.ilchern.saasbilling.messaging.inbox

interface InboxMessageStore {

  fun saveIfAbsent(message: InboxMessage): Boolean
}
