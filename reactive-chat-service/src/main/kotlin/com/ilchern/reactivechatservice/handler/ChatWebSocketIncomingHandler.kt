package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.application.message.IncomingSocketMessageHandler
import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.model.api.IncomingChatSocketMessage
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Component
class ChatWebSocketIncomingHandler(
  private val chatEventCodec: ChatEventCodec,
  private val incomingSocketMessageHandler: IncomingSocketMessageHandler,
  private val errorHandler: ChatWebSocketErrorHandler,
) {

  fun handle(
    session: WebSocketSession,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return session.receive()
      .flatMap { message -> handleMessage(message, context, outboundSink) }
      .then()
  }

  private fun handleMessage(
    message: WebSocketMessage,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return Mono.fromCallable { chatEventCodec.decode(message.payloadAsText) }
      .flatMap { envelope -> handleDecodedMessage(envelope, context, outboundSink) }
      .onErrorResume { throwable -> errorHandler.incomingDecodeFailure(context, outboundSink, throwable) }
  }

  private fun handleDecodedMessage(
    envelope: IncomingChatSocketMessage,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return incomingSocketMessageHandler.handle(context.chatId, context.userId, envelope)
      .onErrorResume { throwable ->
        errorHandler.incomingProcessingFailure(
          context = context,
          outboundSink = outboundSink,
          correlationId = envelope.correlationId,
          throwable = throwable,
        )
      }
  }
}
