package com.ilchern.reactivechatservice.model.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant

data class ChatEvent(
  val eventId: String = "",
  val eventType: ChatEventType,
  val correlationId: String? = null,
  val chatId: String = "",
  val senderId: String = "",
  val timestamp: Instant = Instant.now(),
  val payload: ChatEventPayload? = null,
)

enum class ChatEventType(
  @get:JsonValue
  val value: String,
) {
  CHAT_MESSAGE_CREATED("chat.message.created"),
  CHAT_MESSAGE_ACCEPTED("chat.message.accepted"),
  CHAT_MESSAGE_DELIVERED("chat.message.delivered"),
  CHAT_MESSAGE_REJECTED("chat.message.rejected"),
  CHAT_TYPING_STARTED("chat.typing.started"),
  CHAT_TYPING_STOPPED("chat.typing.stopped"),
  ERROR("error");

  companion object {
    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): ChatEventType {
      return entries.firstOrNull { it.value == value || it.name == value }
        ?: error("Unsupported eventType=$value")
    }
  }
}
