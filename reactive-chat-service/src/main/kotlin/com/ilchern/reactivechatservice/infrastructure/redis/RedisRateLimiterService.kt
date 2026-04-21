package com.ilchern.reactivechatservice.infrastructure.redis

import com.ilchern.reactivechatservice.application.ratelimit.RateLimitDecision
import com.ilchern.reactivechatservice.application.ratelimit.RateLimiterService
import com.ilchern.reactivechatservice.config.properties.RateLimitProperties
import com.ilchern.reactivechatservice.infrastructure.metrics.RateLimitMetrics
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock

@Service
class RedisRateLimiterService(
  private val redisTemplate: ReactiveStringRedisTemplate,
  private val tokenBucketRedisScript: RedisScript<String>,
  private val rateLimitProperties: RateLimitProperties,
  private val rateLimitMetrics: RateLimitMetrics,
) : RateLimiterService {

  private val clock = Clock.systemUTC()

  override fun tryConsume(userId: String): Mono<RateLimitDecision> {
    if (!rateLimitProperties.enabled) {
      return Mono.just(RateLimitDecision.allowed(Long.MAX_VALUE))
    }

    val redisKey = "${rateLimitProperties.keyPrefix}:$userId"
    val nowMillis = clock.millis()
    val ttlSeconds = rateLimitProperties.ttl.seconds.coerceAtLeast(1)

    return redisTemplate.execute(
      tokenBucketRedisScript,
      listOf(redisKey),
      listOf(
        rateLimitProperties.capacity.toString(),
        rateLimitProperties.refillTokens.toString(),
        rateLimitProperties.refillPeriod.toMillis().toString(),
        "1",
        nowMillis.toString(),
        ttlSeconds.toString(),
      ),
    )
      .single()
      .map(::parseDecision)
      .doOnNext { decision ->
        if (decision.allowed) {
          rateLimitMetrics.recordAllowed()
          log.info(
            "Rate limit allowed: userId={}, remainingTokens={}, retryAfterMillis={}",
            userId,
            decision.remainingTokens,
            decision.retryAfterMillis,
          )
        } else {
          rateLimitMetrics.recordRejected()
          log.warn(
            "Rate limit rejected: userId={}, remainingTokens={}, retryAfterMillis={}",
            userId,
            decision.remainingTokens,
            decision.retryAfterMillis,
          )
        }
      }
      .onErrorResume { error ->
        rateLimitMetrics.recordBackendError()
        log.error(
          "Rate limiter backend failed, rejecting request in fail-closed mode: userId={}, fallbackRetryAfterMillis={}",
          userId,
          rateLimitProperties.refillPeriod.toMillis(),
          error,
        )
        Mono.just(RateLimitDecision.rejected(rateLimitProperties.refillPeriod.toMillis()))
      }
  }

  private fun parseDecision(rawResult: String): RateLimitDecision {
    val parts = rawResult.split(':', limit = 3)
    check(parts.size == 3) { "Unexpected rate limiter result: $rawResult" }

    val allowed = parts[0] == "1"
    val remainingTokens = parts[1].toLong()
    val retryAfterMillis = parts[2].toLong()

    return RateLimitDecision(
      allowed = allowed,
      remainingTokens = remainingTokens,
      retryAfterMillis = retryAfterMillis,
    )
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}
