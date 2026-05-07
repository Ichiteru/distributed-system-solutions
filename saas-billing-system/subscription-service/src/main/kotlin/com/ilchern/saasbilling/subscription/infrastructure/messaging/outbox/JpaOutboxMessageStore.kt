package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionDomainEvent
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

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
      payload = objectMapper.convertValue(event, PAYLOAD_TYPE_REFERENCE),
      headers = buildHeaders(event),
      timestamp = LocalDateTime.ofInstant(event.occurredAt, ZoneOffset.UTC),
    )

  private fun buildHeaders(event: SubscriptionDomainEvent): Map<String, Any> =
    mapOf(
      "messageId" to event.eventId.toString(),
      "schemaVersion" to SCHEMA_VERSION
    )

  companion object {
    private const val AGGREGATE_TYPE = "subscription"
    private const val SCHEMA_VERSION = 1
    private val PAYLOAD_TYPE_REFERENCE = object : TypeReference<Map<String, Any>>() {}
  }
}
