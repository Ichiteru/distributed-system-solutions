package com.ilchern.reactivechatservice.handler

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class ChatWebSockerHandler : WebSocketHandler {
  override fun handle(session: WebSocketSession): Mono<Void?> {
    TODO("Not yet implemented")
  }
}