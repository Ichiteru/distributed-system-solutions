package com.ilchern.reactivechatservice.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OutboundBufferProperties::class)
class BackpressureConfig
