package com.ilchern.saasbilling.payment.domain.event

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttemptId
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import java.time.Instant
import java.util.UUID

data class PaymentSucceededEvent(
  override val eventId: UUID = UUID.randomUUID(),
  override val type: String = TYPE,
  override val paymentAttemptId: PaymentAttemptId,
  override val invoiceId: InvoiceId,
  override val subscriptionId: SubscriptionId,
  override val organizationId: OrganizationId,
  override val amount: Money,
  override val providerPaymentReference: ProviderPaymentReference,
  override val attemptNumber: Int,
  override val occurredAt: Instant,
) : PaymentDomainEvent {
  companion object {
    const val TYPE = "PaymentSucceededEvent"
  }
}
