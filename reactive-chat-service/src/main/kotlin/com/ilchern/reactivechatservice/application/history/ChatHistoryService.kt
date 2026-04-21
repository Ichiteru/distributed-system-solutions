package com.ilchern.reactivechatservice.application.history

import com.ilchern.reactivechatservice.application.event.ChatEventFactory
import com.ilchern.reactivechatservice.config.properties.HistoryProperties
import com.ilchern.reactivechatservice.model.api.ChatEventEnvelope
import com.ilchern.reactivechatservice.infrastructure.persistence.mongo.ChatMessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

interface ChatHistoryService {
  fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope>
}

@Service
class DefaultChatHistoryService(
  private val chatMessageRepository: ChatMessageRepository,
  private val historyProperties: HistoryProperties,
  private val chatEventFactory: ChatEventFactory,
) : ChatHistoryService {

  override fun loadRecentHistory(chatId: String): Flux<ChatEventEnvelope> {
    val pageRequest = PageRequest.of(
      0,
      historyProperties.reconnectLimit,
      Sort.by(Sort.Direction.DESC, "payload.createdAt"),
    )

    return chatMessageRepository.findByChatId(chatId, pageRequest)
      .collectList()
      .flatMapMany { messages -> Flux.fromIterable(messages.asReversed()) }
      .map(chatEventFactory::messageCreated)
  }
}
