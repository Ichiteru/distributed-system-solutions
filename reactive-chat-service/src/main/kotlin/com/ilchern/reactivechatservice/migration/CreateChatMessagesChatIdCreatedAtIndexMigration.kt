package com.ilchern.reactivechatservice.migration

import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@ChangeUnit(
  id = "create-chat-messages-chat-id-created-at-index",
  order = "002",
  author = "reactive-chat-service",
)
class CreateChatMessagesChatIdCreatedAtIndexMigration(
  private val mongoTemplate: MongoTemplate,
) {

  @Execution
  fun execute() {
    mongoTemplate.indexOps(CHAT_MESSAGES_COLLECTION)
      .createIndex(
        Index()
          .on("chatId", Sort.Direction.ASC)
          .on("payload.createdAt", Sort.Direction.DESC)
          .named(CHAT_MESSAGES_INDEX),
      )
  }

  @RollbackExecution
  fun rollback() {
  }

  private companion object {
    const val CHAT_MESSAGES_COLLECTION = "chat_messages"
    const val CHAT_MESSAGES_INDEX = "idx_chat_messages_chat_id_created_at_desc"
  }
}
