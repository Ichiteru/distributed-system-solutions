package com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka

import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import com.ilchern.saasbilling.subscription.application.command.ActivateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.command.SuspendSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.handler.ActivateSubscriptionHandler
import com.ilchern.saasbilling.subscription.application.handler.SuspendSubscriptionHandler
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.*

@Component
class SubscriptionCommandListener(
  private val clock: Clock,
  private val envelopeReader: OutboxMessageEnvelopeReader,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val activateSubscriptionHandler: ActivateSubscriptionHandler,
  private val suspendSubscriptionHandler: SuspendSubscriptionHandler,
) {

  @KafkaListener(
    topics = ["\${subscription.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "subscriptionCommandKafkaListenerContainerFactory",
  )
  fun onMessage(record: GenericRecord) {
    val envelope = envelopeReader.read(record)
    when (envelope.type) {
      ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE -> handleActivateSubscription(envelope)
      SUSPEND_SUBSCRIPTION_MESSAGE_TYPE -> handleSuspendSubscription(envelope)
      else -> error("Unsupported subscription command type: ${envelope.type}")
    }
  }

  private fun handleActivateSubscription(envelope: OutboxMessageEnvelope) {
    val context = parseContext(
      envelope = envelope,
      consumer = ACTIVATE_SUBSCRIPTION_CONSUMER,
      messageType = ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE,
    )

    process(context, envelope) {
      activateSubscriptionHandler.handle(
        ActivateSubscriptionCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
          messageId = envelope.id,
          correlationId = optionalUuid(envelope.headers["correlationId"]),
          causationId = optionalUuid(envelope.headers["causationId"]),
          occurredAt = envelope.timestamp,
        ),
      )
    }
  }

  private fun handleSuspendSubscription(envelope: OutboxMessageEnvelope) {
    val context = parseContext(
      envelope = envelope,
      consumer = SUSPEND_SUBSCRIPTION_CONSUMER,
      messageType = SUSPEND_SUBSCRIPTION_MESSAGE_TYPE,
    )

    process(context, envelope) {
      suspendSubscriptionHandler.handle(
        SuspendSubscriptionCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
          messageId = envelope.id,
          correlationId = optionalUuid(envelope.headers["correlationId"]),
          causationId = optionalUuid(envelope.headers["causationId"]),
          occurredAt = envelope.timestamp,
        ),
      )
    }
  }

  private fun process(
    context: CommandContext,
    envelope: OutboxMessageEnvelope,
    action: () -> Unit,
  ) {
    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = context.consumer,
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

  private fun parseContext(
    envelope: OutboxMessageEnvelope,
    consumer: String,
    messageType: String,
  ): CommandContext {
    require(envelope.type == messageType) {
      "Unsupported subscription command type: ${envelope.type}"
    }
    require(envelope.aggregateType == AGGREGATE_TYPE) {
      "Unsupported aggregate type ${envelope.aggregateType}"
    }

    val subscriptionId = UUID.fromString(requiredString(envelope.payload, "subscriptionId"))
    require(envelope.aggregateId == subscriptionId.toString()) {
      "aggregateId ${envelope.aggregateId} does not match subscriptionId $subscriptionId"
    }

    return CommandContext(
      consumer = consumer,
      subscriptionId = subscriptionId,
      organizationId = requiredString(envelope.payload, "organizationId"),
    )
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

  private fun scalarString(value: Any): String =
    when (value) {
      is Map<*, *> -> requireNotNull(value["value"]) { "Missing value in command payload scalar wrapper" }.toString()
      else -> value.toString()
    }

  private fun optionalUuid(value: Any?): UUID? =
    value?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

  private data class CommandContext(
    val consumer: String,
    val subscriptionId: UUID,
    val organizationId: String,
  )

  companion object {
    private const val ACTIVATE_SUBSCRIPTION_CONSUMER = "subscription.activate-subscription"
    private const val ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE = "ActivateSubscription"
    private const val MARK_SUBSCRIPTION_PAST_DUE_CONSUMER = "subscription.mark-subscription-past-due"
    private const val MARK_SUBSCRIPTION_PAST_DUE_MESSAGE_TYPE = "MarkSubscriptionPastDue"
    private const val SUSPEND_SUBSCRIPTION_CONSUMER = "subscription.suspend-subscription"
    private const val SUSPEND_SUBSCRIPTION_MESSAGE_TYPE = "SuspendSubscription"
    private const val CANCEL_SUBSCRIPTION_AT_PERIOD_END_CONSUMER = "subscription.cancel-subscription-at-period-end"
    private const val CANCEL_SUBSCRIPTION_AT_PERIOD_END_MESSAGE_TYPE = "CancelSubscriptionAtPeriodEnd"
    private const val AGGREGATE_TYPE = "subscription"
  }
}
