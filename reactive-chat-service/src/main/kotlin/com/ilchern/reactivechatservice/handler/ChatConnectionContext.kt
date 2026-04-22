package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.model.domain.ChatParticipantRole

data class ChatConnectionContext(
  val sessionId: String,
  val userId: String,
  val chatId: String,
  val role: ChatParticipantRole,
)
