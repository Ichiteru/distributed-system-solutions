package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.model.api.ChatMessageRequest
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.EventTypes
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.model.event.ChatMessageEvent
import com.ilchern.reactivechatservice.model.event.PayloadEvent
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

interface ChatMessageService {
  fun create(request: ChatMessageRequest): Mono<ChatMessage>
}

@Service
class DefaultChatMessageService(
  private val chatMessageRepository: ChatMessageRepository,
  private val redisChatEventPublisher: RedisChatEventPublisher,
) : ChatMessageService {

  override fun create(request: ChatMessageRequest): Mono<ChatMessage> {
    return chatMessageRepository.save(
      ChatMessage(
        eventType = EventTypes.CHAT_MESSAGE_CREATED,
        chatId = request.chatId,
        senderId = request.senderId,
        correlationId = "", // TODO
        payload = Payload(
          type = request.payload.type,
          value = request.payload.value,
          createdAt = LocalDateTime.now()
        )
      )
    )
      .delayUntil { chatMessage ->
        redisChatEventPublisher.publishCreated(
          ChatMessageEvent(
            id = chatMessage.id,
            eventType = chatMessage.eventType,
            correlationId = chatMessage.correlationId,
            chatId = chatMessage.chatId,
            senderId = chatMessage.senderId,
            payload = PayloadEvent(
              type = chatMessage.payload.type,
              value = chatMessage.payload.value,
              createdAt = chatMessage.payload.createdAt
            )
          )
        )
      }
  }
}