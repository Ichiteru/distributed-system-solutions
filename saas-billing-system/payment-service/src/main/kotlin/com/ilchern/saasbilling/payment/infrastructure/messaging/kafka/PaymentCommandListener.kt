package com.ilchern.saasbilling.payment.infrastructure.messaging.kafka

import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import com.ilchern.saasbilling.payment.application.command.SubmitPaymentCommand
import com.ilchern.saasbilling.payment.application.handler.SubmitPaymentHandler
import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import java.time.Clock
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentCommandListener(
  private val clock: Clock,
  private val envelopeReader: OutboxMessageEnvelopeReader,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val submitPaymentHandler: SubmitPaymentHandler,
) {

  @KafkaListener(
    topics = ["\${payment.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "paymentCommandKafkaListenerContainerFactory",
  )
  fun onMessage(record: GenericRecord) {
    val envelope = envelopeReader.read(record)
    when (envelope.type) {
      SUBMIT_PAYMENT_MESSAGE_TYPE -> handleSubmitPayment(envelope)
      else -> error("Unsupported payment command type: ${envelope.type}")
    }
  }

  private fun handleSubmitPayment(envelope: OutboxMessageEnvelope) {
    val invoiceId = UUID.fromString(requiredString(envelope.payload, "invoiceId"))

    process(envelope) {
      submitPaymentHandler.handle(
        SubmitPaymentCommand(
          invoiceId = InvoiceId(invoiceId),
          subscriptionId = SubscriptionId(UUID.fromString(requiredString(envelope.payload, "subscriptionId"))),
          organizationId = OrganizationId(requiredString(envelope.payload, "organizationId")),
          amount = Money(
            amountMinor = requiredLong(envelope.payload, "amountMinor"),
            currency = requiredString(envelope.payload, "currency"),
          ),
          paymentMethodToken = PaymentMethodToken(requiredString(envelope.payload, "paymentMethodToken")),
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
        consumer = SUBMIT_PAYMENT_CONSUMER,
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
      ?: error("Missing command payload field $field")

  private fun requiredLong(
    payload: Map<String, Any>,
    field: String,
  ): Long {
    val value = requireNotNull(payload[field]) { "Missing command payload field $field" }
    return when (value) {
      is Number -> value.toLong()
      else -> value.toString().toLong()
    }
  }

  private fun scalarString(value: Any): String =
    when (value) {
      is Map<*, *> -> requireNotNull(value["value"]) { "Missing value in command payload scalar wrapper" }.toString()
      else -> value.toString()
    }

  private fun optionalUuid(value: Any?): UUID? =
    value?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

  companion object {
    private const val SUBMIT_PAYMENT_CONSUMER = "payment.submit-payment"
    private const val SUBMIT_PAYMENT_MESSAGE_TYPE = "SubmitPayment"
  }
}
