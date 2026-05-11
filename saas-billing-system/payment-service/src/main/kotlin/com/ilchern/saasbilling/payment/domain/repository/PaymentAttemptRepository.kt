package com.ilchern.saasbilling.payment.domain.repository

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt

interface PaymentAttemptRepository {
  fun save(paymentAttempt: PaymentAttempt): PaymentAttempt

  fun findLatestByInvoiceId(invoiceId: InvoiceId): PaymentAttempt?
}
