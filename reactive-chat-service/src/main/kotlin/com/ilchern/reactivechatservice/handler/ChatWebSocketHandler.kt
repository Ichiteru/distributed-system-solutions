package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.model.domain.ChatParticipantRole
import com.ilchern.reactivechatservice.application.history.ChatHistoryService
import com.ilchern.reactivechatservice.application.message.IncomingSocketMessageHandler
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistrationRequest
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Locale

@Component
class ChatWebSocketHandler(
  private val chatEventCodec: ChatEventCodec,
  private val sessionRegistry: SessionRegistry,
  private val incomingSocketMessageHandler: IncomingSocketMessageHandler,
  private val chatHistoryService: ChatHistoryService,
) : WebSocketHandler {

  override fun handle(session: WebSocketSession): Mono<Void> {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND")
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT ID NOT FOUND")
    val role = queryParams.getFirst("role")
      ?.uppercase(Locale.getDefault())
      ?.let(ChatParticipantRole::valueOf)
      ?: error("ROLE NOT FOUND")

    val sessionSink = sessionRegistry.register(
      SessionRegistrationRequest(
        sessionId = session.id,
        userId = userId,
        chatId = chatId,
        role = role,
      )
    )
    val history = chatHistoryService.loadRecentHistory(chatId)
      .map(chatEventCodec::encode)
      .map { payload -> OutboundMessage(payload, OutboundMessagePriority.CRITICAL) }

    val outgoing = session.send(
      Flux.concat(history, sessionSink.asFlux())
        .map { outboundMessage -> session.textMessage(outboundMessage.payload) }
    )

    val incoming = session.receive()
      .map { message -> chatEventCodec.decode(message.payloadAsText) }
      .flatMap { envelope -> incomingSocketMessageHandler.handle(chatId, userId, envelope) }
      .then()

    return outgoing
      .and(incoming)
      .doFinally { sessionRegistry.remove(session.id) }
  }
}
