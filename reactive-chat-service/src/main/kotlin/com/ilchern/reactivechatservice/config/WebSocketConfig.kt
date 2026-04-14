package com.ilchern.reactivechatservice.config

import com.ilchern.reactivechatservice.handler.ChatWebSockerHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class WebSocketConfig(
  private val chatWebSockerHandler: ChatWebSockerHandler,
) {


  @Bean
  fun webSockerHandlerMapping() : HandlerMapping {
    return SimpleUrlHandlerMapping(
      mapOf(
        "/ws/chat" to chatWebSockerHandler
      ),
      1
    )
  }
}