package com.ilchern.saasbilling.payment.application.command

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

data class SubmitPaymentCommand(
  val invoiceId: InvoiceId,
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val amount: Money,
  val paymentMethodToken: PaymentMethodToken,
  val messageId: UUID,
  val correlationId: UUID?,
  val causationId: UUID?,
  val occurredAt: Instant,
)
