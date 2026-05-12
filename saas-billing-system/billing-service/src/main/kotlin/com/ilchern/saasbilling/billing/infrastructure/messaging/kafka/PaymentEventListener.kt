package com.ilchern.saasbilling.billing.infrastructure.messaging.kafka

import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaidCommand
import com.ilchern.saasbilling.billing.application.handler.MarkInvoicePaidHandler
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import java.time.Clock
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentEventListener(
  private val clock: Clock,
  private val envelopeReader: OutboxMessageEnvelopeReader,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val markInvoicePaidHandler: MarkInvoicePaidHandler,
) {

  @KafkaListener(
    topics = ["\${billing.kafka.topics.payment-events.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "billingCommandKafkaListenerContainerFactory",
  )
  fun onMessage(record: GenericRecord) {
    val envelope = envelopeReader.read(record)
    when (envelope.type) {
      PAYMENT_SUCCEEDED_EVENT_TYPE -> handlePaymentSucceeded(envelope)
      else -> error("Unsupported payment event type: ${envelope.type}")
    }
  }

  private fun handlePaymentSucceeded(envelope: OutboxMessageEnvelope) {
    val invoiceId = UUID.fromString(requiredString(envelope.payload, "invoiceId"))
    require(envelope.aggregateId == invoiceId.toString()) {
      "aggregateid ${envelope.aggregateId} does not match invoiceId $invoiceId"
    }

    process(envelope) {
      markInvoicePaidHandler.handle(
        MarkInvoicePaidCommand(
          invoiceId = InvoiceId(invoiceId),
          amount = Money(
            amountMinor = requiredLong(envelope.payload, "amountMinor"),
            currency = requiredString(envelope.payload, "currency"),
          ),
          paymentAttemptId = UUID.fromString(requiredString(envelope.payload, "paymentAttemptId")),
          providerPaymentId = requiredString(envelope.payload, "providerPaymentId"),
          messageId = envelope.id,
          correlationId = optionalUuid(envelope.headers["correlationId"]),
          causationId = optionalUuid(envelope.headers["causationId"]),
          occurredAt = envelope.timestamp,
        ),
      )
    }
  }

  private fun process(
    envelope: OutboxMessageEnvelope,
    action: () -> Unit,
  ) {
    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = PAYMENT_SUCCEEDED_CONSUMER,
        messageId = envelope.id,
        messageType = envelope.type,
        aggregateId = envelope.aggregateId,
        correlationId = optionalUuid(envelope.headers["correlationId"]),
        causationId = optionalUuid(envelope.headers["causationId"]),
        receivedAt = clock.instant(),
        payload = envelope.payload,
        headers = envelopeHeaders(envelope),
      ),
    ) {
      action()
    }
  }

  private fun envelopeHeaders(envelope: OutboxMessageEnvelope): Map<String, Any> =
    envelope.headers + mapOf(
      "id" to envelope.id.toString(),
      "type" to envelope.type,
      "aggregateid" to envelope.aggregateId,
      "aggregatetype" to envelope.aggregateType,
      "timestamp" to envelope.timestamp.toString(),
    )

  private fun requiredString(
    payload: Map<String, Any>,
    field: String,
  ): String =
    payload[field]?.let(::scalarString)
      ?: error("Missing payment event payload field $field")

  private fun requiredLong(
    payload: Map<String, Any>,
    field: String,
  ): Long {
    val value = requireNotNull(payload[field]) { "Missing payment event payload field $field" }
    return when (value) {
      is Number -> value.toLong()
      else -> value.toString().toLong()
    }
  }

  private fun scalarString(value: Any): String =
    when (value) {
      is Map<*, *> -> requireNotNull(value["value"]) { "Missing value in payment event payload scalar wrapper" }.toString()
      else -> value.toString()
    }

  private fun optionalUuid(value: Any?): UUID? =
    value?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

  companion object {
    private const val PAYMENT_SUCCEEDED_CONSUMER = "billing.payment-succeeded"
    private const val PAYMENT_SUCCEEDED_EVENT_TYPE = "PaymentSucceededEvent"
  }
}
