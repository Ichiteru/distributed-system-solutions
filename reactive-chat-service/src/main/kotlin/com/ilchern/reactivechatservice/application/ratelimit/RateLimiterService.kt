package com.ilchern.reactivechatservice.application.ratelimit

import reactor.core.publisher.Mono

interface RateLimiterService {
  fun tryConsume(userId: String): Mono<RateLimitDecision>
}

data class RateLimitDecision(
  val allowed: Boolean,
  val remainingTokens: Long,
  val retryAfterMillis: Long,
) {
  companion object {
    fun allowed(remainingTokens: Long): RateLimitDecision {
      return RateLimitDecision(
        allowed = true,
        remainingTokens = remainingTokens,
        retryAfterMillis = 0,
      )
    }

    fun rejected(retryAfterMillis: Long): RateLimitDecision {
      return RateLimitDecision(
        allowed = false,
        remainingTokens = 0,
        retryAfterMillis = retryAfterMillis,
      )
    }
  }
}
