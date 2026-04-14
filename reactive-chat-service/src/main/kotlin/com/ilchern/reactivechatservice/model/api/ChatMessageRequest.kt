package com.ilchern.reactivechatservice.model.api

data class ChatMessageRequest(
  val eventType: String,
  val correlationId: String,
  val chatId: String,
  val senderId: String,
  val payload: Payload,
)

data class Payload(
  val type: String,
  val value: String,
)
