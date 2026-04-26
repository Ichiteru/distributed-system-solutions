package com.ilchern.saasbilling.subscription.domain.model

import com.ilchern.saasbilling.subscription.domain.event.SubscriptionCancellationRequestedEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionChangeScheduledEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionCreatedEvent
import com.ilchern.saasbilling.subscription.domain.event.SubscriptionDomainEvent
import java.time.Instant
import java.util.UUID

@JvmInline
value class SubscriptionId(val value: UUID)

@JvmInline
value class OrganizationId(val value: String)

@JvmInline
value class PaymentMethodToken(val value: String)

class Subscription private constructor(
  val id: SubscriptionId,
  val organizationId: OrganizationId,
  val createdAt: Instant,
  private var status: SubscriptionStatus,
  private var subscriptionPlan: SubscriptionPlan,
  private var billingPeriod: BillingPeriod,
  private var seats: Int,
  private val paymentMethodToken: PaymentMethodToken,
  private var pendingSubscriptionChange: SubscriptionChange?,
  private var domainEvents: MutableList<SubscriptionDomainEvent>,
  private var historyEntries: MutableList<SubscriptionHistoryEntry>,
){

  fun seats() = seats
  fun subscriptionPlan() = subscriptionPlan
  fun billingPeriod() = billingPeriod
  fun status() = status
  fun paymentMethodToken() = paymentMethodToken
  fun pendingSubscriptionChange() = pendingSubscriptionChange
  fun historyEntries() = historyEntries.toList()

  fun pullDomainEvents() = domainEvents


  fun cancelAtPeriodEnd(occurredAt: Instant) : Subscription {

    require(status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAST_DUE) {
      "Subscription must be active or past due"
    }

    status = SubscriptionStatus.CANCEL_AT_PERIOD_END

    historyEntries += SubscriptionHistoryEntry(
      action = "SUBSCRIPTION_CANCELLATION_AT_PERIOD_END_REQUESTED",
      occurredAt = occurredAt,
    )
    domainEvents += SubscriptionCancellationRequestedEvent(
      subscriptionId = id,
      organizationId = organizationId,
      occurredAt = occurredAt,
    )

    return this
  }

  fun scheduleChange(
    newSeats: Int,
    newPlan: SubscriptionPlan,
    requestedAt: Instant,
  ) : Subscription {

    require(status == SubscriptionStatus.ACTIVE) { "Subscription must be active" }
    require(newSeats > 0) { "New seats must be positive" }

    historyEntries += SubscriptionHistoryEntry(
      action = "SUBSCRIPTION_CHANGE_REQUESTED",
      occurredAt = requestedAt,
      details = mapOf(
        "newSeats" to newSeats.toString(),
        "newPlan" to newPlan.name,
      ),
    )

    pendingSubscriptionChange = SubscriptionChange(
      requestedAt = requestedAt,
      newPlan = newPlan,
      newSeats = newSeats,
    )
    domainEvents += SubscriptionChangeScheduledEvent(
      subscriptionId = id,
      organizationId = organizationId,
      occurredAt = requestedAt,
      newPlan = newPlan,
      newSeats = newSeats,
    )

    return this
  }

  companion object {

    fun create(
      subscriptionPlan: SubscriptionPlan,
      billingPeriod: BillingPeriod,
      seats: Int,
      organizationId: OrganizationId,
      paymentMethodToken: PaymentMethodToken,
      requestedAt: Instant,
    ) : Subscription {

      require(seats > 0) { "Seats must be positive" }

      val subscriptionId = SubscriptionId(UUID.randomUUID())

      val subscription = Subscription(
        id = subscriptionId,
        organizationId = organizationId,
        createdAt = requestedAt,
        status = SubscriptionStatus.PENDING,
        subscriptionPlan = subscriptionPlan,
        billingPeriod = billingPeriod,
        seats = seats,
        paymentMethodToken = paymentMethodToken,
        pendingSubscriptionChange = null,
        domainEvents = mutableListOf(
          SubscriptionCreatedEvent(
            subscriptionId = subscriptionId,
            organizationId = organizationId,
            occurredAt = requestedAt,
            paymentMethodToken = paymentMethodToken,
            billingPeriod = billingPeriod,
            seats = seats,
            subscriptionPlan = subscriptionPlan,
          ),
        ),
        historyEntries = mutableListOf(
          SubscriptionHistoryEntry(
            action = "SUBSCRIPTION_CREATED",
            occurredAt = requestedAt,
            details = mapOf(
              "plan" to subscriptionPlan.name,
              "billingPeriod" to billingPeriod.name,
              "seats" to seats.toString(),
            ),
          )
        ),
      )

      return subscription
    }

    fun copy(
      id: SubscriptionId,
      organizationId: OrganizationId,
      createdAt: Instant,
      status: SubscriptionStatus,
      subscriptionPlan: SubscriptionPlan,
      billingPeriod: BillingPeriod,
      seats: Int,
      paymentMethodToken: PaymentMethodToken,
      pendingSubscriptionChange: SubscriptionChange?,
      historyEntries: List<SubscriptionHistoryEntry>,
    ): Subscription =
      Subscription(
        id = id,
        organizationId = organizationId,
        createdAt = createdAt,
        status = status,
        subscriptionPlan = subscriptionPlan,
        billingPeriod = billingPeriod,
        seats = seats,
        paymentMethodToken = paymentMethodToken,
        pendingSubscriptionChange = pendingSubscriptionChange,
        domainEvents = mutableListOf(),
        historyEntries = historyEntries.toMutableList(),
      )
  }
}
