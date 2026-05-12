package com.ilchern.saasbilling.payment.domain.repository

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference

interface PaymentAttemptRepository {
  fun save(paymentAttempt: PaymentAttempt): PaymentAttempt

  fun findLatestByInvoiceId(invoiceId: InvoiceId): PaymentAttempt?

  fun findByProviderPaymentReference(providerPaymentReference: ProviderPaymentReference): PaymentAttempt?
}
