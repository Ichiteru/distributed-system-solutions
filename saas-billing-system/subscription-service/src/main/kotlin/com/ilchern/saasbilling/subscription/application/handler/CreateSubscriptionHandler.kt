package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.CreateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.infrastructure.persistence.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class CreateSubscriptionHandler(
  // TODO почему clock?
  private val clock: Clock,
  private val subscriptionRepository: SubscriptionRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  // todo добавить ключ идемпотентности
  fun handle(command: CreateSubscriptionCommand) : Subscription {
    val subscription = Subscription.create(
      subscriptionPlan = command.plan,
      billingPeriod = command.billingPeriod,
      seats = command.seats,
      organizationId = command.organizationId,
      paymentMethodToken = command.paymentMethodToken,
      requestedAt = clock.instant()
    )

    subscriptionRepository.save(subscription)
    outboxMessageStore.append(subscription.pullDomainEvents())
    return subscription
  }
}