package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import com.ilchern.saasbilling.messaging.outbox.OutboxMessage
import com.ilchern.saasbilling.messaging.outbox.TransactionalOutboxMessageStore
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionActivatedEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionCancellationRequestedEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionChangeScheduledEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionCreatedEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionDomainEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionSuspendedEvent
import org.springframework.stereotype.Repository

@Repository
class JpaOutboxMessageStore(
  private val transactionalOutboxMessageStore: TransactionalOutboxMessageStore,
) : OutboxMessageStore {

  override fun append(events: List<SubscriptionDomainEvent>) =
    transactionalOutboxMessageStore.append(events.map(::toOutboxMessage))

  private fun toOutboxMessage(event: SubscriptionDomainEvent): OutboxMessage =
    OutboxMessage(
      id = event.eventId,
      aggregateType = AGGREGATE_TYPE,
      aggregateId = event.subscriptionId.value.toString(),
      type = event.type,
      payload = buildPayload(event),
      headers = buildHeaders(event),
      occurredAt = event.occurredAt,
    )

  private fun buildPayload(event: SubscriptionDomainEvent): Map<String, Any> =
    when (event) {
      is SubscriptionCreatedEvent -> mapOf(
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "subscriptionPlan" to event.subscriptionPlan.name,
        "billingPeriod" to event.billingPeriod.name,
        "seats" to event.seats,
        "paymentMethodToken" to event.paymentMethodToken.value,
      )
      is SubscriptionActivatedEvent -> mapOf(
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "activatedAt" to event.activatedAt.toString(),
      )
      is SubscriptionCancellationRequestedEvent -> mapOf(
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
      )
      is SubscriptionChangeScheduledEvent -> mapOf(
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "newPlan" to event.newPlan.name,
        "newSeats" to event.newSeats,
      )
      is SubscriptionSuspendedEvent -> mapOf(
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "suspendedAt" to event.suspendedAt.toString(),
      )
      else -> error("Unsupported subscription event type: ${event.javaClass.name}")
    }

  private fun buildHeaders(event: SubscriptionDomainEvent): Map<String, Any> =
    mapOf(
      "messageId" to event.eventId.toString(),
      "messageType" to event.type,
      "aggregateId" to event.subscriptionId.value.toString(),
      "aggregateType" to AGGREGATE_TYPE,
      "occurredAt" to event.occurredAt.toString(),
      "schemaVersion" to SCHEMA_VERSION,
    )

  companion object {
    private const val AGGREGATE_TYPE = "subscription"
    private const val SCHEMA_VERSION = 1
  }
}
