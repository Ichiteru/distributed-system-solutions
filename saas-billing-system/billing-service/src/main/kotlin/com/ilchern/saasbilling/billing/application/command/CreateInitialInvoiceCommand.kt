package com.ilchern.saasbilling.billing.application.command

import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import java.time.Instant
import java.util.UUID

data class CreateInitialInvoiceCommand(
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val subscriptionPlan: SubscriptionPlan,
  val billingPeriod: BillingPeriod,
  val seats: Int,
  val paymentMethodToken: PaymentMethodToken,
  val messageId: UUID,
  val correlationId: UUID?,
  val causationId: UUID?,
  val occurredAt: Instant,
)
