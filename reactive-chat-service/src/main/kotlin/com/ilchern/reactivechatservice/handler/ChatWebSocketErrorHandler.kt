package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.infrastructure.event.ChatEventCodec
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import com.ilchern.reactivechatservice.model.domain.ChatErrorPayload
import com.ilchern.reactivechatservice.model.dto.ChatEvent
import com.ilchern.reactivechatservice.model.dto.ChatEventType
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

@Component
class ChatWebSocketErrorHandler(
  private val chatEventCodec: ChatEventCodec,
) {

  fun historyLoadFailure(chatId: String, throwable: Throwable): Mono<OutboundMessage> {
    log.warn("Failed to load recent chat history for chatId={}", chatId, throwable)

    return Mono.just(
      errorOutboundMessage(
        chatId = chatId,
        code = HISTORY_LOAD_ERROR,
        message = HISTORY_LOAD_ERROR_MESSAGE,
      )
    )
  }

  fun incomingDecodeFailure(
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
    throwable: Throwable,
  ): Mono<Void> {
    log.warn(
      "Failed to decode incoming WebSocket message for sessionId={}, chatId={}, userId={}",
      context.sessionId,
      context.chatId,
      context.userId,
      throwable,
    )

    return emitError(
      outboundSink = outboundSink,
      message = errorOutboundMessage(
        chatId = context.chatId,
        senderId = context.userId,
        code = INCOMING_MESSAGE_DECODING_ERROR,
        httpStatus = BAD_REQUEST_STATUS,
        message = INCOMING_MESSAGE_DECODING_ERROR_MESSAGE,
      ),
    )
  }

  fun incomingProcessingFailure(
    context: ChatConnectionContext,
    outboundSink: Sinks.Many<OutboundMessage>,
    correlationId: String?,
    throwable: Throwable,
  ): Mono<Void> {
    log.warn(
      "Failed to process incoming WebSocket message for sessionId={}, chatId={}, userId={}, correlationId={}",
      context.sessionId,
      context.chatId,
      context.userId,
      correlationId,
      throwable,
    )

    return emitError(
      outboundSink = outboundSink,
      message = errorOutboundMessage(
        chatId = context.chatId,
        senderId = context.userId,
        correlationId = correlationId,
        code = INCOMING_MESSAGE_PROCESSING_ERROR,
        httpStatus = INTERNAL_SERVER_ERROR_STATUS,
        message = INCOMING_MESSAGE_PROCESSING_ERROR_MESSAGE,
      ),
    )
  }

  private fun emitError(
    outboundSink: Sinks.Many<OutboundMessage>,
    message: OutboundMessage,
  ): Mono<Void> {
    return Mono.fromRunnable<Void> {
      val emitResult = outboundSink.tryEmitNext(message)

      if (emitResult.isFailure) {
        log.warn("Failed to emit WebSocket error message, emitResult={}", emitResult)
      }
    }
  }

  private fun errorOutboundMessage(
    chatId: String,
    senderId: String = "",
    correlationId: String? = null,
    code: String,
    httpStatus: Int = INTERNAL_SERVER_ERROR_STATUS,
    message: String,
  ): OutboundMessage {
    return OutboundMessage(
      payload = chatEventCodec.encode(
        ChatEvent(
          eventType = ChatEventType.ERROR,
          correlationId = correlationId,
          chatId = chatId,
          senderId = senderId,
          payload = ChatErrorPayload(
            code = code,
            httpStatus = httpStatus,
            message = message,
          ),
        )
      ),
      priority = OutboundMessagePriority.CRITICAL,
    )
  }

  private companion object {
    private val log = LogManager.getLogger()

    private const val BAD_REQUEST_STATUS = 400
    private const val INTERNAL_SERVER_ERROR_STATUS = 500
    private const val HISTORY_LOAD_ERROR = "HISTORY_LOAD_ERROR"
    private const val HISTORY_LOAD_ERROR_MESSAGE = "Failed to load recent chat history"
    private const val INCOMING_MESSAGE_DECODING_ERROR = "INCOMING_MESSAGE_DECODING_ERROR"
    private const val INCOMING_MESSAGE_DECODING_ERROR_MESSAGE = "Failed to decode incoming message"
    private const val INCOMING_MESSAGE_PROCESSING_ERROR = "INCOMING_MESSAGE_PROCESSING_ERROR"
    private const val INCOMING_MESSAGE_PROCESSING_ERROR_MESSAGE = "Failed to process incoming message"
  }
}
