package com.ilchern.reactivechatservice.application.event

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import org.springframework.stereotype.Component

@Component
class ChatEventCodec(
  private val objectMapper: ObjectMapper,
) {

  fun encode(event: ChatEventEnvelope): String {
    return objectMapper.writeValueAsString(event)
  }

  fun decode(payload: String): ChatEventEnvelope {
    return objectMapper.readValue(payload, ChatEventEnvelope::class.java)
  }

  fun <T> decodePayload(event: ChatEventEnvelope, payloadType: Class<T>): T {
    return objectMapper.treeToValue(event.payload, payloadType)
  }

  fun encodePayload(payload: Any): JsonNode {
    return objectMapper.valueToTree(payload)
  }
}
