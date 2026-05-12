package com.ilchern.saasbilling.payment.infrastructure.messaging.outbox

import com.ilchern.saasbilling.messaging.outbox.OutboxMessage
import com.ilchern.saasbilling.messaging.outbox.TransactionalOutboxMessageStore
import com.ilchern.saasbilling.payment.application.port.OutboxMessageStore
import com.ilchern.saasbilling.payment.domain.event.PaymentDomainEvent
import com.ilchern.saasbilling.payment.domain.event.PaymentFailedEvent
import com.ilchern.saasbilling.payment.domain.event.PaymentSucceededEvent
import org.springframework.stereotype.Repository

@Repository
class JpaOutboxMessageStore(
  private val transactionalOutboxMessageStore: TransactionalOutboxMessageStore,
) : OutboxMessageStore {

  override fun append(events: List<PaymentDomainEvent>) =
    transactionalOutboxMessageStore.append(events.map(::toOutboxMessage))

  private fun toOutboxMessage(event: PaymentDomainEvent): OutboxMessage =
    OutboxMessage(
      id = event.eventId,
      aggregateType = AGGREGATE_TYPE,
      aggregateId = event.invoiceId.value.toString(),
      type = event.type,
      payload = buildPayload(event),
      headers = buildHeaders(event),
      occurredAt = event.occurredAt,
    )

  private fun buildPayload(event: PaymentDomainEvent): Map<String, Any> =
    when (event) {
      is PaymentSucceededEvent -> basePayload(event)
      is PaymentFailedEvent -> basePayload(event) + buildMap {
        event.failureCode?.let { put("failureCode", it) }
        event.failureMessage?.let { put("failureMessage", it) }
      }
    }

  private fun basePayload(event: PaymentDomainEvent): Map<String, Any> =
    mapOf(
      "paymentAttemptId" to event.paymentAttemptId.value.toString(),
      "invoiceId" to event.invoiceId.value.toString(),
      "subscriptionId" to event.subscriptionId.value.toString(),
      "organizationId" to event.organizationId.value,
      "amountMinor" to event.amount.amountMinor,
      "currency" to event.amount.currency,
      "providerPaymentId" to event.providerPaymentReference.value,
      "attemptNumber" to event.attemptNumber,
      "occurredAt" to event.occurredAt.toString(),
    )

  private fun buildHeaders(event: PaymentDomainEvent): Map<String, Any> =
    mapOf(
      "messageId" to event.eventId.toString(),
      "messageType" to event.type,
      "aggregateId" to event.invoiceId.value.toString(),
      "aggregateType" to AGGREGATE_TYPE,
      "occurredAt" to event.occurredAt.toString(),
      "schemaVersion" to SCHEMA_VERSION,
    )

  companion object {
    private const val AGGREGATE_TYPE = "payment"
    private const val SCHEMA_VERSION = 1
  }
}
