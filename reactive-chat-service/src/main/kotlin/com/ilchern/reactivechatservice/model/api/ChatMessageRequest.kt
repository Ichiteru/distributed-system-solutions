package com.ilchern.reactivechatservice.model.api

import com.ilchern.reactivechatservice.model.domain.ChatMessageState

data class ChatMessageRequest(
  val type: String,
  val value: String,
  val correlationId: String,
)


data class ChatMessageCallback(
  val correlationId: String?,
  val state: ChatMessageState
)