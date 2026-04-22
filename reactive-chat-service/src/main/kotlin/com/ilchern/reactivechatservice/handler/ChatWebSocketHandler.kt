package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistry
import com.ilchern.reactivechatservice.infrastructure.websocket.session.SessionRegistrationRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Component
class ChatWebSocketHandler(
  private val connectionContextFactory: ChatWebSocketConnectionContextFactory,
  private val sessionRegistry: SessionRegistry,
  private val incomingHandler: ChatWebSocketIncomingHandler,
  private val outgoingHandler: ChatWebSocketOutgoingHandler,
) : WebSocketHandler {

  override fun handle(session: WebSocketSession): Mono<Void> {
    val context = connectionContextFactory.create(session)
    val outboundSink = register(context)

    val outgoing = outgoingHandler.handle(
      session = session,
      context = context,
      outboundSink = outboundSink,
    )
    val incoming = incomingHandler.handle(
      session = session,
      context = context,
      outboundSink = outboundSink,
    )

    return outgoing
      .and(incoming)
      .doFinally { sessionRegistry.remove(context.sessionId) }
  }

  private fun register(context: ChatConnectionContext): Sinks.Many<OutboundMessage> {
    return sessionRegistry.register(
      SessionRegistrationRequest(
        sessionId = context.sessionId,
        userId = context.userId,
        chatId = context.chatId,
        role = context.role,
      )
    )
  }
}
