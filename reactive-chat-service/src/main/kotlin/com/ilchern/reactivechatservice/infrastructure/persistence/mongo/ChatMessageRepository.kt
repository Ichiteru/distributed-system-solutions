package com.ilchern.reactivechatservice.infrastructure.persistence.mongo

import com.ilchern.reactivechatservice.model.domain.ChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : ReactiveMongoRepository<ChatMessage, String> {
  fun findByChatId(chatId: String, pageable: Pageable): reactor.core.publisher.Flux<ChatMessage>
}
