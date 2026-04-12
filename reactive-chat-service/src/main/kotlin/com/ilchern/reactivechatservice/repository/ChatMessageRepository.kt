package com.ilchern.reactivechatservice.repository

import com.ilchern.reactivechatservice.model.domain.ChatMessage
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : ReactiveMongoRepository<ChatMessage, String> {

}