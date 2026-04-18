package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.config.RateLimitProperties
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Clock

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

@Service
class RedisRateLimiterService(
  private val redisTemplate: ReactiveStringRedisTemplate,
  private val tokenBucketRedisScript: RedisScript<String>,
  private val rateLimitProperties: RateLimitProperties,
  meterRegistry: MeterRegistry,
) : RateLimiterService {

  private val requestCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "allowed",
  )
  private val rejectedCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "rejected",
  )
  private val backendErrorCounter = meterRegistry.counter(
    "chat_rate_limit_requests_total",
    "outcome",
    "backend_error",
  )
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
          requestCounter.increment()
          log.info(
            "Rate limit allowed: userId={}, remainingTokens={}, retryAfterMillis={}",
            userId,
            decision.remainingTokens,
            decision.retryAfterMillis,
          )
        } else {
          rejectedCounter.increment()
          log.warn(
            "Rate limit rejected: userId={}, remainingTokens={}, retryAfterMillis={}",
            userId,
            decision.remainingTokens,
            decision.retryAfterMillis,
          )
        }
      }
      .onErrorResume { error ->
        backendErrorCounter.increment()
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
