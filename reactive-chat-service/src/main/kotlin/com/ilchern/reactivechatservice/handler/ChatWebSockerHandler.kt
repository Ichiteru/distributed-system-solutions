package com.ilchern.reactivechatservice.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.EventTypes
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import com.ilchern.reactivechatservice.service.ChatMessageService
import com.ilchern.reactivechatservice.service.RedisChatEventPublisher
import com.ilchern.reactivechatservice.service.WebSocketSessionRegistry
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class ChatWebSockerHandler(
  private val objectMapper: ObjectMapper,
  private val sessionRegistry: WebSocketSessionRegistry,
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
  private val chatMessageService: ChatMessageService,
) : WebSocketHandler {

  override fun handle(session: WebSocketSession): Mono<Void> {

    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND")
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT IN NOT FOUND")

    val incoming = session.receive()
          .flatMap { message ->
            val request = objectMapper.readValue(message.payloadAsText, ChatMessageRequest::class.java)
            chatMessageService.create(request)
          }
          .doFinally { sessionRegistry.remove(userId, chatId) } // TODO будет ли сработывать на каждом событии?
          .then()



    return Mono.fromSupplier { sessionRegistry.put(userId, chatId, session) }
      .then(incoming)
      .then()
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}
