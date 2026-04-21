package com.ilchern.reactivechatservice.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.mongodb.core.MongoTemplate

@ChangeUnit(
  id = "create-chat-messages-collection",
  order = "001",
  author = "reactive-chat-service",
)
class CreateChatMessagesCollectionMigration(
  private val mongoTemplate: MongoTemplate,
) {

  @Execution
  fun execute() {
    if (!mongoTemplate.collectionExists(CHAT_MESSAGES_COLLECTION)) {
      mongoTemplate.createCollection(CHAT_MESSAGES_COLLECTION)
    }
  }

  @RollbackExecution
  fun rollback() {
  }

  private companion object {
    const val CHAT_MESSAGES_COLLECTION = "chat_messages"
  }
}
