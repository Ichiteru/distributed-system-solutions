package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.application.event.ChatEventCodec
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class ChatMessageWriter(
  private val chatMessageRepository: ChatMessageRepository,
  private val chatEventCodec: ChatEventCodec,
) {

  fun save(envelope: ChatEventEnvelope): Mono<ChatMessage> {
    val request = chatEventCodec.decodePayload(envelope, ChatMessagePayload::class.java)

    return chatMessageRepository.save(
      ChatMessage(
        chatId = envelope.chatId,
        senderId = envelope.senderId,
        correlationId = envelope.correlationId ?: envelope.eventId,
        payload = Payload(
          type = request.type,
          value = request.value,
          createdAt = LocalDateTime.now(),
        ),
      )
    )
  }
}
