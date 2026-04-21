package com.ilchern.reactivechatservice.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "chat.rate-limit")
data class RateLimitProperties(
  val enabled: Boolean = true,
  val keyPrefix: String = "chat:rate-limit:user",
  val capacity: Long = 20,
  val refillTokens: Long = 20,
  val refillPeriod: Duration = Duration.ofSeconds(1),
  val ttl: Duration = Duration.ofMinutes(2),
)
