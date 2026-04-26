package com.ilchern.saasbilling.subscription.application.handler

import com.ilchern.saasbilling.subscription.application.command.CreateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.port.IdempotencyKeyStore
import com.ilchern.saasbilling.subscription.application.port.OutboxMessageStore
import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.infrastructure.persistence.repository.SubscriptionRepository
import java.time.Clock
import org.springframework.stereotype.Service

@Service
class CreateSubscriptionHandler(
  private val clock: Clock,
  private val subscriptionRepository: SubscriptionRepository,
  private val idempotencyKeyStore: IdempotencyKeyStore,
  private val outboxMessageStore: OutboxMessageStore,
) {

  fun handle(command: CreateSubscriptionCommand) : Subscription {
    require(command.idempotencyKey.isNotBlank()) { "Idempotency-Key must not be blank" }

    idempotencyKeyStore.findSubscriptionId(
      organizationId = command.organizationId,
      operation = CREATE_SUBSCRIPTION_OPERATION,
      idempotencyKey = command.idempotencyKey,
    )
      ?.let { subscriptionId ->
      return subscriptionRepository.findById(subscriptionId)
        ?: error("Idempotency record points to missing subscription ${subscriptionId.value}")
    }

    val subscription = Subscription.create(
      subscriptionPlan = command.plan,
      billingPeriod = command.billingPeriod,
      seats = command.seats,
      organizationId = command.organizationId,
      paymentMethodToken = command.paymentMethodToken,
      requestedAt = clock.instant()
    )

    subscriptionRepository.save(subscription)
    idempotencyKeyStore.save(
      organizationId = command.organizationId,
      operation = CREATE_SUBSCRIPTION_OPERATION,
      idempotencyKey = command.idempotencyKey,
      subscriptionId = subscription.id,
    )
    outboxMessageStore.append(subscription.pullDomainEvents())
    return subscription
  }

  companion object {
    private const val CREATE_SUBSCRIPTION_OPERATION = "create-subscription"
  }
}
