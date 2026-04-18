package com.ilchern.reactivechatservice.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatParticipantRole
import com.ilchern.reactivechatservice.service.ChatMessageService
import com.ilchern.reactivechatservice.service.SessionRegistry
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.util.Locale

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
    queryParams.getFirst("role")
      ?.uppercase(Locale.getDefault())
      ?.let(ChatParticipantRole::valueOf)
      ?: error("ROLE NOT FOUND")

    val sessionSink = sessionRegistry.register(session)

    val outgoing = session.send(
      sessionSink.asFlux()
        .map { outboundMessage -> session.textMessage(outboundMessage.payload) }
    )

    val incoming = session.receive()
      .map { message -> objectMapper.readValue(message.payloadAsText, ChatEventEnvelope::class.java) }
      .flatMap { envelope -> chatMessageService.handleIncoming(chatId, userId, envelope) }
      .then()

    return outgoing
      .and(incoming)
      .doFinally { sessionRegistry.remove(session.id) }
  }
}
