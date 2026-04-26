package com.ilchern.saasbilling.subscription.domain.event

import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import java.time.Instant
import java.util.UUID

data class SubscriptionCreatedEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val occurredAt: Instant,
  val subscriptionPlan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val paymentMethodToken: PaymentMethodToken,
): SubscriptionDomainEvent
