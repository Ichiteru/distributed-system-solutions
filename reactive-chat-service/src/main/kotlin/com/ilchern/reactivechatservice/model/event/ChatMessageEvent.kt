package com.ilchern.reactivechatservice.model.event

import java.time.LocalDateTime

data class ChatMessageEvent(
  val id: String?,
  val correlationId: String,
  val chatId: String,
  val senderId: String,
  val payload: PayloadEvent,
)

data class PayloadEvent(
  val type: String,
  val value: String,
  val createdAt: LocalDateTime,
)

