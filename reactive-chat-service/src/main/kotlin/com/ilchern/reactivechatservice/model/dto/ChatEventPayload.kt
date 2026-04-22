package com.ilchern.reactivechatservice.model.dto

sealed interface ChatEventPayload

data class ChatMessagePayload(
  val type: String,
  val value: String,
  val messageId: String? = null,
) : ChatEventPayload

data class ChatMessageStatusPayload(
  val messageId: String? = null,
  val status: String,
) : ChatEventPayload

data class ChatErrorPayload(
  val code: String,
  val httpStatus: Int,
  val message: String,
) : ChatEventPayload

enum class ChatParticipantRole {
  CLIENT,
  OPERATOR,
}
