package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.ScheduleSubscriptionChangeCommand
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.domain.repository.SubscriptionRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScheduleSubscriptionChangeHandler(
  private val clock: Clock,
  private val subscriptionRepository: SubscriptionRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: ScheduleSubscriptionChangeCommand): Subscription {
    val subscription = subscriptionRepository.findById(command.subscriptionId)
      ?: error("Subscription ${command.subscriptionId.value} not found")

    subscription.scheduleChange(
      newSeats = command.newSeats,
      newPlan = command.newPlan,
      requestedAt = clock.instant(),
    )

    subscriptionRepository.save(subscription)
    outboxMessageStore.append(subscription.pullDomainEvents())
    return subscription
  }
}
