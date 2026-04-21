package com.ilchern.reactivechatservice.infrastructure.event

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

data class WireChatEventEnvelope(
  val eventId: String = "",
  val eventType: String,
  val correlationId: String? = null,
  val chatId: String = "",
  val senderId: String = "",
  val timestamp: Instant = Instant.now(),
  val payload: JsonNode? = null,
)
