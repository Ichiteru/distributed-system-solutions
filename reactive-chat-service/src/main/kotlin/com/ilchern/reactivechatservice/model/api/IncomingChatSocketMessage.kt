package com.ilchern.reactivechatservice.model.api

import java.time.Instant

data class IncomingChatSocketMessage(
  val eventId: String = "",
  val eventType: ChatEventType,
  val correlationId: String? = null,
  val timestamp: Instant = Instant.now(),
  val payload: IncomingChatSocketMessagePayload,
)

data class IncomingChatSocketMessagePayload(
  val type: String,
  val value: String,
)
