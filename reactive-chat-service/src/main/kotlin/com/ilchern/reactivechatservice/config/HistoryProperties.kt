package com.ilchern.reactivechatservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chat.history")
data class HistoryProperties(
  val reconnectLimit: Int = 100,
)
