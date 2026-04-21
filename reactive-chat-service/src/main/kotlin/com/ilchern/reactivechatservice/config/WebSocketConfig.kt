package com.ilchern.reactivechatservice.config

import com.ilchern.reactivechatservice.handler.ChatWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class WebSocketConfig(
  private val chatWebSocketHandler: ChatWebSocketHandler,
) {


  @Bean
  fun webSockerHandlerMapping() : HandlerMapping {
    return SimpleUrlHandlerMapping(
      mapOf(
        "/ws/chat" to chatWebSocketHandler
      ),
      1
    )
  }
}