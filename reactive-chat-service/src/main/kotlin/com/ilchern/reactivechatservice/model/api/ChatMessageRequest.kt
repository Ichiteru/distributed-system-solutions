package com.ilchern.reactivechatservice.model.api

data class ChatMessageRequest(
  val type: String,
  val value: String,
  val correlationId: String,
)


data class ChatMessageCallback(
  val correlationId: String?,
  val success: Boolean = true,
  val error: String? =  null
)