package com.ilchern.reactivechatservice.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
  value = [
    OutboundBufferProperties::class,
    HistoryProperties::class,
    RateLimitProperties::class
  ]
)
class BackpressureConfig
