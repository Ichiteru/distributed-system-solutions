package com.ilchern.reactivechatservice.application.message

import com.ilchern.reactivechatservice.model.api.ChatEvent
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.model.domain.Payload
import com.ilchern.reactivechatservice.infrastructure.persistence.mongo.ChatMessageRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class ChatMessageWriter(
  private val chatMessageRepository: ChatMessageRepository,
) {

  fun save(envelope: ChatEvent): Mono<ChatMessage> {
    val request = requireNotNull(envelope.payload as? ChatMessagePayload) {
      "Expected ${ChatMessagePayload::class.simpleName} for ${envelope.eventType}"
    }

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
