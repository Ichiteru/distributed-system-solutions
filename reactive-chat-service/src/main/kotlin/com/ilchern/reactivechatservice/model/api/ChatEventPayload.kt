package com.ilchern.reactivechatservice.model.api

data class ChatMessagePayload(
  val type: String,
  val value: String,
  val messageId: String? = null,
)

data class ChatMessageStatusPayload(
  val messageId: String? = null,
  val status: String,
)

data class ChatErrorPayload(
  val code: String,
  val httpStatus: Int,
  val message: String,
)

enum class ChatParticipantRole {
  CLIENT,
  OPERATOR,
}
