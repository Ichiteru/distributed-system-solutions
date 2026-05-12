package com.ilchern.saasbilling.billing.infrastructure.messaging.outbox

import com.ilchern.saasbilling.billing.application.port.OutboxMessageStore
import com.ilchern.saasbilling.billing.domain.event.BillingDomainEvent
import com.ilchern.saasbilling.billing.domain.event.InvoiceCreatedEvent
import com.ilchern.saasbilling.billing.domain.event.InvoicePaidEvent
import com.ilchern.saasbilling.messaging.outbox.OutboxMessage
import com.ilchern.saasbilling.messaging.outbox.TransactionalOutboxMessageStore
import org.springframework.stereotype.Repository

@Repository
class JpaOutboxMessageStore(
  private val transactionalOutboxMessageStore: TransactionalOutboxMessageStore,
) : OutboxMessageStore {

  override fun append(events: List<BillingDomainEvent>) =
    transactionalOutboxMessageStore.append(events.map(::toOutboxMessage))

  private fun toOutboxMessage(event: BillingDomainEvent): OutboxMessage =
    OutboxMessage(
      id = event.eventId,
      aggregateType = AGGREGATE_TYPE,
      aggregateId = event.invoiceId.value.toString(),
      type = event.type,
      payload = buildPayload(event),
      headers = buildHeaders(event),
      occurredAt = event.occurredAt,
    )

  private fun buildPayload(event: BillingDomainEvent): Map<String, Any> =
    when (event) {
      is InvoiceCreatedEvent -> mapOf(
        "invoiceId" to event.invoiceId.value.toString(),
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "invoiceType" to event.invoiceType.name,
        "status" to event.status.name,
        "plan" to event.subscriptionPlan.name,
        "billingPeriod" to event.billingPeriod.name,
        "seats" to event.seats,
        "periodStart" to event.periodStart.toString(),
        "periodEnd" to event.periodEnd.toString(),
        "amountMinor" to event.amount.amountMinor,
        "currency" to event.amount.currency,
        "paymentMethodToken" to event.paymentMethodToken.value,
      )
      is InvoicePaidEvent -> mapOf(
        "invoiceId" to event.invoiceId.value.toString(),
        "subscriptionId" to event.subscriptionId.value.toString(),
        "organizationId" to event.organizationId.value,
        "amountMinor" to event.amount.amountMinor,
        "currency" to event.amount.currency,
        "paidAt" to event.paidAt.toString(),
      )
      else -> error("Unsupported billing event type: ${event.javaClass.name}")
    }

  private fun buildHeaders(event: BillingDomainEvent): Map<String, Any> =
    mapOf(
      "messageId" to event.eventId.toString(),
      "messageType" to event.type,
      "aggregateId" to event.invoiceId.value.toString(),
      "aggregateType" to AGGREGATE_TYPE,
      "occurredAt" to event.occurredAt.toString(),
      "schemaVersion" to SCHEMA_VERSION,
    )

  companion object {
    private const val AGGREGATE_TYPE = "invoice"
    private const val SCHEMA_VERSION = 1
  }
}
