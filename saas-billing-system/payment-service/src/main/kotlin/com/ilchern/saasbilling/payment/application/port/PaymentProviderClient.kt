package com.ilchern.saasbilling.payment.application.port

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId

interface PaymentProviderClient {
  fun submitPayment(request: SubmitProviderPaymentRequest): SubmitProviderPaymentResponse
}

data class SubmitProviderPaymentRequest(
  val invoiceId: InvoiceId,
  val subscriptionId: SubscriptionId,
  val organizationId: OrganizationId,
  val amount: Money,
  val paymentMethodToken: PaymentMethodToken,
  val idempotencyKey: String,
)

data class SubmitProviderPaymentResponse(
  val providerPaymentReference: ProviderPaymentReference,
  val providerStatus: String,
)
