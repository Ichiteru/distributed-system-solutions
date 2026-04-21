package com.ilchern.reactivechatservice.config

import com.ilchern.reactivechatservice.config.properties.HistoryProperties
import com.ilchern.reactivechatservice.config.properties.OutboundBufferProperties
import com.ilchern.reactivechatservice.config.properties.RateLimitProperties
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
