package com.ilchern.saasbilling.subscription.domain.model

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
//            actorUserId = requestedBy,
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
