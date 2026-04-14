package com.ilchern.reactivechatservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisPubSubConfig {

  @Bean
  fun listenerContainer(factory: ReactiveRedisConnectionFactory) : ReactiveRedisMessageListenerContainer {
    return ReactiveRedisMessageListenerContainer(factory)
  }

  @Bean
  fun redisTemplate(factory: ReactiveRedisConnectionFactory) : ReactiveRedisTemplate<String, String> {
    return ReactiveRedisTemplate(
      factory,
      RedisSerializationContext
        .newSerializationContext<String, String>(StringRedisSerializer())
        .value(StringRedisSerializer())
        .build()
    )
  }
}
