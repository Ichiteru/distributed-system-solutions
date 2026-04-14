package com.ilchern.reactivechatservice.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import com.ilchern.reactivechatservice.service.ChatMessageService
import com.ilchern.reactivechatservice.service.SessionRegistry
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Component
class ChatWebSockerHandler(
  private val objectMapper: ObjectMapper,
  private val sessionRegistry: SessionRegistry,
  private val chatMessageService: ChatMessageService,
) : WebSocketHandler {

  override fun handle(session: WebSocketSession): Mono<Void> {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND")
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT ID NOT FOUND")
    val outboundSink = Sinks.many().unicast().onBackpressureBuffer<String>()

    val outgoing = session.send( // TODO рефакторинг - вынести в отдельный класс
      outboundSink.asFlux()
        .map(session::textMessage)
    )

    val incoming = session.receive()
      .map { message -> objectMapper.readValue(message.payloadAsText, ChatMessageRequest::class.java) }
      .flatMap { request -> chatMessageService.create(chatId, userId, request) }
      .then()

    return Mono.fromSupplier { sessionRegistry.register(userId, chatId, session.id, outboundSink) }
      .then(outgoing.and(incoming))
      .doFinally { sessionRegistry.remove(session.id) }
  }
}
