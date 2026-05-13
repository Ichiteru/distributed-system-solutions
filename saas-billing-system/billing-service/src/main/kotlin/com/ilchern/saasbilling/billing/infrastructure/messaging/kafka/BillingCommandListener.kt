package com.ilchern.saasbilling.billing.infrastructure.messaging.kafka

import com.ilchern.saasbilling.billing.application.command.CreateInitialInvoiceCommand
import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaidCommand
import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaymentPendingCommand
import com.ilchern.saasbilling.billing.application.handler.CreateInitialInvoiceHandler
import com.ilchern.saasbilling.billing.application.handler.MarkInvoicePaidHandler
import com.ilchern.saasbilling.billing.application.handler.MarkInvoicePaymentPendingHandler
import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import java.time.Clock
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class BillingCommandListener(
  private val clock: Clock,
  private val envelopeReader: OutboxMessageEnvelopeReader,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val createInitialInvoiceHandler: CreateInitialInvoiceHandler,
  private val markInvoicePaidHandler: MarkInvoicePaidHandler,
  private val markInvoicePaymentPendingHandler: MarkInvoicePaymentPendingHandler,
) {

  @KafkaListener(
    topics = ["\${billing.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "billingCommandKafkaListenerContainerFactory",
  )
  fun onMessage(record: GenericRecord) {
    val envelope = envelopeReader.read(record)
    when (envelope.type) {
      CREATE_INITIAL_INVOICE_MESSAGE_TYPE -> handleCreateInitialInvoice(envelope)
      MARK_INVOICE_PAID_MESSAGE_TYPE -> handleMarkInvoicePaid(envelope)
      MARK_INVOICE_PAYMENT_PENDING_MESSAGE_TYPE -> handleMarkInvoicePaymentPending(envelope)
      else -> error("Unsupported billing command type: ${envelope.type}")
    }
  }

  private fun handleCreateInitialInvoice(envelope: OutboxMessageEnvelope) {
    val subscriptionId = UUID.fromString(requiredString(envelope.payload, "subscriptionId"))
    require(envelope.aggregateId == subscriptionId.toString()) {
      "aggregateid ${envelope.aggregateId} does not match subscriptionId $subscriptionId"
    }

    process(envelope, CREATE_INITIAL_INVOICE_CONSUMER) {
      createInitialInvoiceHandler.handle(
        CreateInitialInvoiceCommand(
          subscriptionId = SubscriptionId(subscriptionId),
          organizationId = OrganizationId(requiredString(envelope.payload, "organizationId")),
          subscriptionPlan = SubscriptionPlan.valueOf(requiredString(envelope.payload, "subscriptionPlan", "plan")),
          billingPeriod = BillingPeriod.valueOf(requiredString(envelope.payload, "billingPeriod")),
          seats = requiredInt(envelope.payload, "seats"),
          paymentMethodToken = PaymentMethodToken(requiredString(envelope.payload, "paymentMethodToken")),
          messageId = envelope.id,
          correlationId = optionalUuid(envelope.headers["correlationId"]),
          causationId = optionalUuid(envelope.headers["causationId"]),
          occurredAt = envelope.timestamp,
        ),
      )
    }
  }

  private fun handleMarkInvoicePaid(envelope: OutboxMessageEnvelope) {
    val invoiceId = UUID.fromString(requiredString(envelope.payload, "invoiceId"))
    require(envelope.aggregateId == invoiceId.toString()) {
      "aggregateid ${envelope.aggregateId} does not match invoiceId $invoiceId"
    }

    process(envelope, MARK_INVOICE_PAID_CONSUMER) {
      markInvoicePaidHandler.handle(
        MarkInvoicePaidCommand(
          invoiceId = InvoiceId(invoiceId),
          amount = Money(
            amountMinor = requiredLong(envelope.payload, "amountMinor"),
            currency = requiredString(envelope.payload, "currency"),
          ),
          messageId = envelope.id,
          correlationId = optionalUuid(envelope.headers["correlationId"]),
          causationId = optionalUuid(envelope.headers["causationId"]),
          occurredAt = envelope.timestamp,
        ),
      )
    }
  }

  private fun handleMarkInvoicePaymentPending(envelope: OutboxMessageEnvelope) {
    val invoiceId = UUID.fromString(requiredString(envelope.payload, "invoiceId"))
    require(envelope.aggregateId == invoiceId.toString()) {
      "aggregateid ${envelope.aggregateId} does not match invoiceId $invoiceId"
    }

    process(envelope, MARK_INVOICE_PAYMENT_PENDING_CONSUMER) {
      markInvoicePaymentPendingHandler.handle(
        MarkInvoicePaymentPendingCommand(
          invoiceId = InvoiceId(invoiceId),
          amount = Money(
            amountMinor = requiredLong(envelope.payload, "amountMinor"),
            currency = requiredString(envelope.payload, "currency"),
          ),
          failureCode = optionalString(envelope.payload["failureCode"]),
          failureMessage = optionalString(envelope.payload["failureMessage"]),
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
    consumer: String,
    action: () -> Unit,
  ) {
    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = consumer,
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
    vararg fields: String,
  ): String =
    fields.firstNotNullOfOrNull { field -> payload[field]?.let(::scalarString) }
      ?: error("Missing command payload field ${fields.joinToString(" or ")}")

  private fun requiredInt(
    payload: Map<String, Any>,
    field: String,
  ): Int {
    val value = requireNotNull(payload[field]) { "Missing command payload field $field" }
    return when (value) {
      is Number -> value.toInt()
      else -> value.toString().toInt()
    }
  }

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

  private fun optionalString(value: Any?): String? =
    value?.let(::scalarString)?.takeIf { it.isNotBlank() }

  private fun optionalUuid(value: Any?): UUID? =
    value?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

  companion object {
    private const val CREATE_INITIAL_INVOICE_CONSUMER = "billing.create-initial-invoice"
    private const val CREATE_INITIAL_INVOICE_MESSAGE_TYPE = "CreateInitialInvoice"
    private const val MARK_INVOICE_PAID_CONSUMER = "billing.mark-invoice-paid"
    private const val MARK_INVOICE_PAID_MESSAGE_TYPE = "MarkInvoicePaid"
    private const val MARK_INVOICE_PAYMENT_PENDING_CONSUMER = "billing.mark-invoice-payment-pending"
    private const val MARK_INVOICE_PAYMENT_PENDING_MESSAGE_TYPE = "MarkInvoicePaymentPending"
  }
}
