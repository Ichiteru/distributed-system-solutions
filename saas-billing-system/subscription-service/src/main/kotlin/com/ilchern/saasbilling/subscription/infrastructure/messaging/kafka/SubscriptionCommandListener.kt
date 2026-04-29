package com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka

import com.ilchern.saasbilling.contracts.messaging.subscription.ActivateSubscriptionCommand as ActivateSubscriptionCommandMessage
import com.ilchern.saasbilling.subscription.application.command.ActivateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.handler.ActivateSubscriptionHandler
import com.ilchern.saasbilling.subscription.application.port.InboxMessage
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.infrastructure.messaging.inbox.InboxMessageProcessor
import org.springframework.kafka.annotation.KafkaHandler
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SubscriptionCommandListener(
  private val clock: Clock,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val activateSubscriptionHandler: ActivateSubscriptionHandler,
) {

  @KafkaListener(
    topics = ["\${subscription.kafka.topics.commands.name}"],
    groupId = "\${spring.application.name}",
    containerFactory = "subscriptionCommandKafkaListenerContainerFactory",
  )
  fun onActivateSubscription(message: ActivateSubscriptionCommandMessage) {
    val metadata = requireNotNull(message.metadata) { "metadata must not be null" }

    val subscriptionId = UUID.fromString(message.subscriptionId.toString())


    val organizationId = message.organizationId.toString()
    val messageId = UUID.fromString(metadata.messageId.toString())
    val occurredAt = Instant.parse(metadata.occurredAt.toString())
    val correlationId = metadata.correlationId?.takeIf { it.isNotBlank() }?.let(UUID::fromString)
    val causationId = metadata.causationId?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = ACTIVATE_SUBSCRIPTION_CONSUMER,
        messageId = messageId,
        messageType = ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE,
        aggregateId = subscriptionId.toString(),
        correlationId = correlationId,
        causationId = causationId,
        receivedAt = clock.instant(),
        payload = mapOf(
          "subscriptionId" to subscriptionId.toString(),
          "organizationId" to organizationId,
        ),
        headers = buildHeaders(message),
      ),
    ) {
      activateSubscriptionHandler.handle(
        ActivateSubscriptionCommand(
          subscriptionId = SubscriptionId(subscriptionId),
          organizationId = OrganizationId(organizationId),
          messageId = messageId,
          correlationId = correlationId,
          causationId = causationId,
          occurredAt = occurredAt,
        ),
      )
    }
  }

  private fun buildHeaders(message: ActivateSubscriptionCommandMessage): Map<String, Any> =
    buildMap {
      val metadata = requireNotNull(message.metadata) { "metadata must not be null" }
      put("messageId", metadata.messageId.toString())
      put("messageType", metadata.messageType.toString())
      put("aggregateId", metadata.aggregateId.toString())
      put("aggregateType", metadata.aggregateType.toString())
      put("occurredAt", metadata.occurredAt.toString())
      metadata.correlationId?.takeIf { it.isNotBlank() }?.let { put("correlationId", it) }
      metadata.causationId?.takeIf { it.isNotBlank() }?.let { put("causationId", it) }
      put("schemaVersion", metadata.schemaVersion)
    }

  companion object {
    private const val ACTIVATE_SUBSCRIPTION_CONSUMER = "subscription.activate-subscription"
    private const val ACTIVATE_SUBSCRIPTION_MESSAGE_TYPE = "ActivateSubscription"
  }
}
