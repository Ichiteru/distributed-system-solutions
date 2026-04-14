package com.ilchern.reactivechatservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisPubSubConfig {

  @Bean
  fun listenerContainer(factory: ReactiveRedisConnectionFactory) : ReactiveRedisMessageListenerContainer {
    return ReactiveRedisMessageListenerContainer(factory)
  }

  @Bean
  fun chatMessageEventRedisTemplate(
    factory: ReactiveRedisConnectionFactory,
    objectMapper: ObjectMapper,
  ) : ReactiveRedisTemplate<String, ChatMessageEvent> {
    val keySerializer = StringRedisSerializer()
    val valueSerializer = Jackson2JsonRedisSerializer(ChatMessageEvent::class.java).apply {
      setObjectMapper(objectMapper)
    }

    return ReactiveRedisTemplate(
      factory,
      RedisSerializationContext
        .newSerializationContext<String, ChatMessageEvent>(keySerializer)
        .value(valueSerializer)
        .build()
    )
  }
}
