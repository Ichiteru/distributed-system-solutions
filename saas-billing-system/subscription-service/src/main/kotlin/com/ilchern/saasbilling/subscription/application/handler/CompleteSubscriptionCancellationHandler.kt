package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.CompleteSubscriptionCancellationCommand
import com.ilchern.saasbilling.subscription.domain.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompleteSubscriptionCancellationHandler(
  private val subscriptionRepository: SubscriptionRepository,
) {

  @Transactional
  fun handle(command: CompleteSubscriptionCancellationCommand) {
    val subscription = subscriptionRepository.findById(command.subscriptionId)
      ?: error("Subscription ${command.subscriptionId.value} not found")

    require(subscription.organizationId == command.organizationId) {
      "Subscription ${command.subscriptionId.value} does not belong to organization ${command.organizationId.value}"
    }

    subscription.completeCancellationAtPeriodEnd(command.occurredAt)
    subscriptionRepository.save(subscription)
  }
}
