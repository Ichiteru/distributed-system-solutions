package com.ilchern.saasbilling.billing.infrastructure.messaging.kafka

import com.ilchern.saasbilling.billing.application.command.CreateInitialInvoiceCommand
import com.ilchern.saasbilling.billing.application.handler.CreateInitialInvoiceHandler
import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.contracts.messaging.billing.CreateInitialInvoiceCommand as CreateInitialInvoiceCommandMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.apache.avro.specific.SpecificRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class BillingCommandListener(
  private val clock: Clock,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val createInitialInvoiceHandler: CreateInitialInvoiceHandler,
) {

  @KafkaListener(
    topics = ["\${billing.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "billingCommandKafkaListenerContainerFactory",
  )
  fun onMessage(message: SpecificRecord) {
    when (message) {
      is CreateInitialInvoiceCommandMessage -> handleCreateInitialInvoice(message)
      else -> error("Unsupported billing command type: ${message.schema.fullName}")
    }
  }

  private fun handleCreateInitialInvoice(message: CreateInitialInvoiceCommandMessage) {
    val context = parseContext(
      consumer = CREATE_INITIAL_INVOICE_CONSUMER,
      messageType = CREATE_INITIAL_INVOICE_MESSAGE_TYPE,
      subscriptionId = message.subscriptionId.toString(),
      metadata = message.metadata,
    )

    process(context, message) {
      createInitialInvoiceHandler.handle(
        CreateInitialInvoiceCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(message.organizationId.toString()),
          subscriptionPlan = SubscriptionPlan.valueOf(message.plan.toString()),
          billingPeriod = BillingPeriod.valueOf(message.billingPeriod.toString()),
          seats = message.seats,
          paymentMethodToken = PaymentMethodToken(message.paymentMethodToken.toString()),
          messageId = context.messageId,
          correlationId = context.correlationId,
          causationId = context.causationId,
          occurredAt = context.occurredAt,
        ),
      )
    }
  }

  private fun process(
    context: CommandContext,
    message: CreateInitialInvoiceCommandMessage,
    action: () -> Unit,
  ) {
    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = context.consumer,
        messageId = context.messageId,
        messageType = context.messageType,
        aggregateId = context.subscriptionId.toString(),
        correlationId = context.correlationId,
        causationId = context.causationId,
        receivedAt = clock.instant(),
        payload = mapOf(
          "subscriptionId" to context.subscriptionId.toString(),
          "organizationId" to message.organizationId.toString(),
          "plan" to message.plan.toString(),
          "billingPeriod" to message.billingPeriod.toString(),
          "seats" to message.seats,
          "paymentMethodToken" to message.paymentMethodToken.toString(),
        ),
        headers = buildHeaders(context),
      ),
    ) {
      action()
    }
  }

  private fun parseContext(
    consumer: String,
    messageType: String,
    subscriptionId: String,
    metadata: com.ilchern.saasbilling.contracts.messaging.MessageMetadata?,
  ): CommandContext {
    val requiredMetadata = requireNotNull(metadata) { "metadata must not be null" }
    require(requiredMetadata.messageType.toString() == messageType) {
      "Unsupported billing command type: ${requiredMetadata.messageType}"
    }

    val parsedSubscriptionId = UUID.fromString(subscriptionId)
    require(requiredMetadata.aggregateId.toString() == parsedSubscriptionId.toString()) {
      "aggregateId ${requiredMetadata.aggregateId} does not match subscriptionId $parsedSubscriptionId"
    }
    require(requiredMetadata.aggregateType.toString() == AGGREGATE_TYPE) {
      "Unsupported aggregate type ${requiredMetadata.aggregateType}"
    }

    return CommandContext(
      consumer = consumer,
      messageType = messageType,
      subscriptionId = parsedSubscriptionId,
      messageId = UUID.fromString(requiredMetadata.messageId.toString()),
      correlationId = requiredMetadata.correlationId?.takeIf { it.isNotBlank() }?.let(UUID::fromString),
      causationId = requiredMetadata.causationId?.takeIf { it.isNotBlank() }?.let(UUID::fromString),
      occurredAt = Instant.parse(requiredMetadata.occurredAt.toString()),
      schemaVersion = requiredMetadata.schemaVersion,
    )
  }

  private fun buildHeaders(context: CommandContext): Map<String, Any> =
    buildMap {
      put("messageId", context.messageId.toString())
      put("messageType", context.messageType)
      put("aggregateId", context.subscriptionId.toString())
      put("aggregateType", AGGREGATE_TYPE)
      put("occurredAt", context.occurredAt.toString())
      context.correlationId?.let { put("correlationId", it.toString()) }
      context.causationId?.let { put("causationId", it.toString()) }
      put("schemaVersion", context.schemaVersion)
    }

  private data class CommandContext(
    val consumer: String,
    val messageType: String,
    val subscriptionId: UUID,
    val messageId: UUID,
    val correlationId: UUID?,
    val causationId: UUID?,
    val occurredAt: Instant,
    val schemaVersion: Int,
  )

  companion object {
    private const val CREATE_INITIAL_INVOICE_CONSUMER = "billing.create-initial-invoice"
    private const val CREATE_INITIAL_INVOICE_MESSAGE_TYPE = "CreateInitialInvoice"
    private const val AGGREGATE_TYPE = "subscription"
  }
}
