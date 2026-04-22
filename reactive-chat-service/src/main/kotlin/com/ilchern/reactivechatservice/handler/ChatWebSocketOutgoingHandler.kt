package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.application.history.ChatHistoryService
import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Component
class ChatWebSocketOutgoingHandler(
  private val chatEventCodec: ChatEventCodec,
  private val chatHistoryService: ChatHistoryService,
  private val errorHandler: ChatWebSocketErrorHandler,
) {

  fun handle(
    session: WebSocketSession,
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Mono<Void> {
    return session.send(
      messages(context, outboundSink)
        .map { outboundMessage -> session.textMessage(outboundMessage.payload) }
    )
  }

  private fun messages(
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
  ): Flux<OutboundMessage> {
    return Flux.concat(recentHistoryMessages(context.chatId), outboundSink.asFlux())
  }

  private fun recentHistoryMessages(chatId: String): Flux<OutboundMessage> {
    return chatHistoryService.loadRecentHistory(chatId)
      .map(chatEventCodec::encode)
      .map { payload -> OutboundMessage(payload, OutboundMessagePriority.CRITICAL) }
      .onErrorResume { throwable -> errorHandler.historyLoadFailure(chatId, throwable) }
  }
}
