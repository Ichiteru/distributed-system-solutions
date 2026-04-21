package com.ilchern.reactivechatservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.reactivechatservice.config.properties.HistoryProperties
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.model.api.ChatEventType
import com.ilchern.reactivechatservice.model.api.ChatMessagePayload
import com.ilchern.reactivechatservice.model.domain.ChatMessage
import com.ilchern.reactivechatservice.repository.ChatMessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.ZoneOffset
import java.util.UUID

interface ChatHistoryService {
  fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope>
}

@Service
class DefaultChatHistoryService(
  private val chatMessageRepository: ChatMessageRepository,
  private val historyProperties: HistoryProperties,
  private val objectMapper: ObjectMapper,
) : ChatHistoryService {

  override fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope> {
    val pageRequest = PageRequest.of(
      0,
      historyProperties.reconnectLimit,
      Sort.by(Sort.Direction.DESC, "payload.createdAt"),
    )

    return chatMessageRepository.findByChatId(chatId, pageRequest)
      .collectList()
      .flatMapMany { messages ->
        Flux.fromIterable(messages.asReversed())
      }
      .map(::buildMessageCreatedEnvelope)
  }

  private fun buildMessageCreatedEnvelope(chatMessage: ChatMessage): ChatEventEnvelope {
    return ChatEventEnvelope(
      eventId = UUID.randomUUID().toString(),
      eventType = ChatEventType.CHAT_MESSAGE_CREATED,
      correlationId = chatMessage.correlationId,
      chatId = chatMessage.chatId,
      senderId = chatMessage.senderId,
      timestamp = chatMessage.payload.createdAt.atZone(ZoneOffset.UTC).toInstant(),
      payload = objectMapper.valueToTree(
        ChatMessagePayload(
          type = chatMessage.payload.type,
          value = chatMessage.payload.value,
          messageId = chatMessage.id,
        )
      ),
    )
  }
}
