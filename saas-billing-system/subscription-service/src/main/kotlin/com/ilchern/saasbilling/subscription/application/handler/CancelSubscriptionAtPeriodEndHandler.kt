package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.CancelSubscriptionAtPeriodEndCommand
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.domain.repository.SubscriptionRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelSubscriptionAtPeriodEndHandler(
  private val clock: Clock,
  private val subscriptionRepository: SubscriptionRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: CancelSubscriptionAtPeriodEndCommand): Subscription {
    val subscription = subscriptionRepository.findById(command.subscriptionId)
      ?: error("Subscription ${command.subscriptionId.value} not found")

    subscription.cancelAtPeriodEnd(clock.instant())

    subscriptionRepository.save(subscription)
    outboxMessageStore.append(subscription.pullDomainEvents())
    return subscription
  }
}
