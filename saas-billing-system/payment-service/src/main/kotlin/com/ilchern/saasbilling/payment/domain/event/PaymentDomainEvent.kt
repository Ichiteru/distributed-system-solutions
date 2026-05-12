package com.ilchern.saasbilling.payment.domain.event

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttemptId
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

sealed interface PaymentDomainEvent {
  val eventId: UUID
  val type: String
  val paymentAttemptId: PaymentAttemptId
  val invoiceId: InvoiceId
  val subscriptionId: SubscriptionId
  val organizationId: OrganizationId
  val amount: Money
  val providerPaymentReference: ProviderPaymentReference
  val attemptNumber: Int
  val occurredAt: Instant
}
