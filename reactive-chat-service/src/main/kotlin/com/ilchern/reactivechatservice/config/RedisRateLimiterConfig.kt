package com.ilchern.reactivechatservice.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scripting.support.ResourceScriptSource

@Configuration
@EnableConfigurationProperties(RateLimitProperties::class)
class RedisRateLimiterConfig {

  @Bean
  fun reactiveStringRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveStringRedisTemplate {
    return ReactiveStringRedisTemplate(factory)
  }

  @Bean
  fun tokenBucketRedisScript(): DefaultRedisScript<String> {
    return DefaultRedisScript<String>().apply {
      setScriptSource(ResourceScriptSource(ClassPathResource("scripts/token_bucket.lua")))
      resultType = String::class.java
    }
  }
}
