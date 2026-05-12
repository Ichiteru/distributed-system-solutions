package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.ActivateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivateSubscriptionHandler(
  private val subscriptionRepository: SubscriptionRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: ActivateSubscriptionCommand) {
    val subscription = subscriptionRepository.findById(command.subscriptionId)
      ?: error("Subscription ${command.subscriptionId.value} not found")

    require(subscription.organizationId == command.organizationId) {
      "Subscription ${command.subscriptionId.value} does not belong to organization ${command.organizationId.value}"
    }

    subscription.activate(command.occurredAt)
    subscriptionRepository.save(subscription)
    outboxMessageStore.append(subscription.pullDomainEvents())
  }
}
