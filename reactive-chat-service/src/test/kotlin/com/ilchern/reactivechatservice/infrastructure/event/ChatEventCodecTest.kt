package com.ilchern.reactivechatservice.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessagePayload
import com.ilchern.reactivechatservice.model.domain.ChatMessagePayload
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChatEventCodecTest {

  private val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
  private val codec = ChatEventCodec(objectMapper)

  @Test
  fun `decodes incoming message event type from wire value`() {
    val payload = """
      {
        "eventType": "chat.message.created",
        "correlationId": "test",
        "payload": {
          "type": "TEXT",
          "value": "Hello!"
        }
      }
    """.trimIndent()

    val message = codec.decode(payload)

    assertThat(message.eventType).isEqualTo(ChatEventType.CHAT_MESSAGE_CREATED)
  }

  @Test
  fun `encodes event type as wire value`() {
    val payload = objectMapper.writeValueAsString(
      IncomingChatSocketMessage(
        eventType = ChatEventType.CHAT_MESSAGE_CREATED,
        correlationId = "test",
        payload = IncomingChatSocketMessagePayload(
          type = "TEXT",
          value = "Hello!",
        ),
      )
    )

    assertThat(payload).contains("\"eventType\":\"chat.message.created\"")
  }

  @Test
  fun `encodes outgoing event type as wire value`() {
    val payload = codec.encode(
      ChatEvent(
        eventType = ChatEventType.CHAT_MESSAGE_CREATED,
        correlationId = "test",
        payload = ChatMessagePayload(
          type = "TEXT",
          value = "Hello!",
        ),
      )
    )

    assertThat(payload).contains("\"eventType\":\"chat.message.created\"")
  }
}
