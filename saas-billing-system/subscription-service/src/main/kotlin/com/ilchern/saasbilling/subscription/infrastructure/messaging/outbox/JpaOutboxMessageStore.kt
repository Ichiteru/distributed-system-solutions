package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import com.fasterxml.jackson.databind.node.ObjectNode
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
      aggregateType = AGGREGATE_TYPE,
      aggregateId = event.subscriptionId.value.toString(),
      type = event.javaClass.simpleName,
      payload = objectMapper.valueToTree(event),
      headers = buildHeaders(event),
      timestamp = event.occurredAt,
    )

  private fun buildHeaders(event: SubscriptionDomainEvent): Map<String, Any> =
    mapOf(
      "messageId" to event.eventId.toString(),
      "schemaVersion" to SCHEMA_VERSION
    )

  companion object {
    private const val AGGREGATE_TYPE = "subscription"
    private const val SCHEMA_VERSION = 1
  }
}
