package com.ilchern.reactivechatservice.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class ChatWebSockerHandler(
  private val objectMapper: ObjectMapper,
) : WebSocketHandler {


  override fun handle(session: WebSocketSession): Mono<Void> {

    val incoming = session.receive()
      .map { message ->
        log.info("Message = {}, type = {}", message.payloadAsText, message.type)
        objectMapper.readValue(message.payloadAsText, ChatMessageRequest::class.java)
      }

    return incoming
      .then()
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}