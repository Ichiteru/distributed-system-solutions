package com.ilchern.reactivechatservice.model.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "chat_messages")
data class ChatMessage(
  @Id
  val id: String? = null,
  val correlationId: String,
  val chatId: String,
  val senderId: String,
  val payload: Payload,
)

data class Payload(
  val type: String,
  val value: String,
  val createdAt: LocalDateTime,
)
