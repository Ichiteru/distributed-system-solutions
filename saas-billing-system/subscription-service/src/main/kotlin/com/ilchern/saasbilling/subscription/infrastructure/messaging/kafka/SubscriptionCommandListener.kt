package com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka

import com.ilchern.saasbilling.contracts.messaging.subscription.ActivateSubscriptionCommand as ActivateSubscriptionCommandMessage
import com.ilchern.saasbilling.contracts.messaging.subscription.CancelSubscriptionAtPeriodEndCommand as CancelSubscriptionAtPeriodEndCommandMessage
import com.ilchern.saasbilling.contracts.messaging.subscription.MarkSubscriptionPastDueCommand as MarkSubscriptionPastDueCommandMessage
import com.ilchern.saasbilling.contracts.messaging.subscription.SuspendSubscriptionCommand as SuspendSubscriptionCommandMessage
import com.ilchern.saasbilling.subscription.application.command.ActivateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.command.CompleteSubscriptionCancellationCommand
import com.ilchern.saasbilling.subscription.application.command.MarkSubscriptionPastDueCommand
import com.ilchern.saasbilling.subscription.application.command.SuspendSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.handler.ActivateSubscriptionHandler
import com.ilchern.saasbilling.subscription.application.handler.CompleteSubscriptionCancellationHandler
import com.ilchern.saasbilling.subscription.application.handler.MarkSubscriptionPastDueHandler
import com.ilchern.saasbilling.subscription.application.handler.SuspendSubscriptionHandler
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.apache.avro.specific.SpecificRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SubscriptionCommandListener(
  private val clock: Clock,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val activateSubscriptionHandler: ActivateSubscriptionHandler,
  private val markSubscriptionPastDueHandler: MarkSubscriptionPastDueHandler,
  private val suspendSubscriptionHandler: SuspendSubscriptionHandler,
  private val completeSubscriptionCancellationHandler: CompleteSubscriptionCancellationHandler,
) {

  @KafkaListener(
    topics = ["\${subscription.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "subscriptionCommandKafkaListenerContainerFactory",
  )
  fun onMessage(message: SpecificRecord) {
    when (message) {
      is ActivateSubscriptionCommandMessage -> handleActivateSubscription(message)
      is MarkSubscriptionPastDueCommandMessage -> handleMarkSubscriptionPastDue(message)
      is SuspendSubscriptionCommandMessage -> handleSuspendSubscription(message)
      is CancelSubscriptionAtPeriodEndCommandMessage -> handleCancelSubscriptionAtPeriodEnd(message)
      else -> error("Unsupported subscription command type: ${message.schema.fullName}")
    }
  }

  private fun handleActivateSubscription(message: ActivateSubscriptionCommandMessage) {
    val context = parseContext(
      consumer = ACTIVATE_SUBSCRIPTION_CONSUMER,
      messageType = ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE,
      subscriptionId = message.subscriptionId.toString(),
      organizationId = message.organizationId.toString(),
      metadata = message.metadata,
    )

    process(context) {
      activateSubscriptionHandler.handle(
        ActivateSubscriptionCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
          messageId = context.messageId,
          correlationId = context.correlationId,
          causationId = context.causationId,
          occurredAt = context.occurredAt,
        ),
      )
    }
  }

  private fun handleMarkSubscriptionPastDue(message: MarkSubscriptionPastDueCommandMessage) {
    val context = parseContext(
      consumer = MARK_SUBSCRIPTION_PAST_DUE_CONSUMER,
      messageType = MARK_SUBSCRIPTION_PAST_DUE_MESSAGE_TYPE,
      subscriptionId = message.subscriptionId.toString(),
      organizationId = message.organizationId.toString(),
      metadata = message.metadata,
    )

    process(context) {
      markSubscriptionPastDueHandler.handle(
        MarkSubscriptionPastDueCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
          messageId = context.messageId,
          correlationId = context.correlationId,
          causationId = context.causationId,
          occurredAt = context.occurredAt,
        ),
      )
    }
  }

  private fun handleSuspendSubscription(message: SuspendSubscriptionCommandMessage) {
    val context = parseContext(
      consumer = SUSPEND_SUBSCRIPTION_CONSUMER,
      messageType = SUSPEND_SUBSCRIPTION_MESSAGE_TYPE,
      subscriptionId = message.subscriptionId.toString(),
      organizationId = message.organizationId.toString(),
      metadata = message.metadata,
    )

    process(context) {
      suspendSubscriptionHandler.handle(
        SuspendSubscriptionCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
          messageId = context.messageId,
          correlationId = context.correlationId,
          causationId = context.causationId,
          occurredAt = context.occurredAt,
        ),
      )
    }
  }

  private fun handleCancelSubscriptionAtPeriodEnd(message: CancelSubscriptionAtPeriodEndCommandMessage) {
    val context = parseContext(
      consumer = CANCEL_SUBSCRIPTION_AT_PERIOD_END_CONSUMER,
      messageType = CANCEL_SUBSCRIPTION_AT_PERIOD_END_MESSAGE_TYPE,
      subscriptionId = message.subscriptionId.toString(),
      organizationId = message.organizationId.toString(),
      metadata = message.metadata,
    )

    process(context) {
      completeSubscriptionCancellationHandler.handle(
        CompleteSubscriptionCancellationCommand(
          subscriptionId = SubscriptionId(context.subscriptionId),
          organizationId = OrganizationId(context.organizationId),
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
          "organizationId" to context.organizationId,
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
    organizationId: String,
    metadata: com.ilchern.saasbilling.contracts.messaging.MessageMetadata?,
  ): CommandContext {
    val requiredMetadata = requireNotNull(metadata) { "metadata must not be null" }
    require(requiredMetadata.messageType.toString() == messageType) {
      "Unsupported subscription command type: ${requiredMetadata.messageType}"
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
      organizationId = organizationId,
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
    val organizationId: String,
    val messageId: UUID,
    val correlationId: UUID?,
    val causationId: UUID?,
    val occurredAt: Instant,
    val schemaVersion: Int,
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
