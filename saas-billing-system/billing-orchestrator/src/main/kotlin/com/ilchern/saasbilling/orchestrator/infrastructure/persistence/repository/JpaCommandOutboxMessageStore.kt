package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.repository

import com.ilchern.saasbilling.orchestrator.application.port.CommandOutboxMessage
import com.ilchern.saasbilling.orchestrator.application.port.CommandOutboxMessageStore
import com.ilchern.saasbilling.orchestrator.infrastructure.persistence.entity.CommandOutboxEntity
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.springframework.stereotype.Repository

@Repository
class JpaCommandOutboxMessageStore(
  private val commandOutboxJpaRepository: CommandOutboxJpaRepository,
) : CommandOutboxMessageStore {

  override fun append(message: CommandOutboxMessage) {
    commandOutboxJpaRepository.save(toEntity(message))
  }

  private fun toEntity(message: CommandOutboxMessage): CommandOutboxEntity =
    CommandOutboxEntity(
      id = message.id,
      destinationTopic = message.destinationTopic,
      aggregateType = message.aggregateType,
      aggregateId = message.aggregateId,
      type = message.type,
      payload = message.payload,
      headers = message.headers,
      timestamp = LocalDateTime.ofInstant(message.timestamp, ZoneOffset.UTC),
    )
}
