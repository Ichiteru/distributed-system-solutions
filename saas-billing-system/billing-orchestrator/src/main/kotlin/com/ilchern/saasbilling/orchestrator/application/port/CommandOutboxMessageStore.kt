package com.ilchern.saasbilling.orchestrator.application.port

interface CommandOutboxMessageStore {

  fun append(message: CommandOutboxMessage)
}
