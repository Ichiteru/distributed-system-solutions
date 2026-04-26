package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionDomainEvent
import org.springframework.stereotype.Repository

@Repository
class JpaOutboxMessageStore(
  private val outboxMessageJpaRepository: OutboxMessageJpaRepository,
  private val objectMapper: ObjectMapper,
) : OutboxMessageStore {

  override fun append(events: List<SubscriptionDomainEvent>) {
    if (events.isEmpty()) {
      return
    }

    outboxMessageJpaRepository.saveAll(events.map(::toEntity))
  }

  private fun toEntity(event: SubscriptionDomainEvent): OutboxMessageEntity =
    OutboxMessageEntity(
      id = event.eventId,
      messageType = event.javaClass.simpleName,
      aggregateId = event.subscriptionId.value,
      aggregateType = AGGREGATE_TYPE,
      occurredAt = event.occurredAt,
      schemaVersion = SCHEMA_VERSION,
      payload = objectMapper.writeValueAsString(event),
    )

  companion object {
    private const val AGGREGATE_TYPE = "subscription"
    private const val SCHEMA_VERSION = 1
  }
}
