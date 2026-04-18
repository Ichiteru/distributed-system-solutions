package com.ilchern.reactivechatservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chat.outbound")
data class OutboundBufferProperties(
  val bufferSize: Int = 256,
)
