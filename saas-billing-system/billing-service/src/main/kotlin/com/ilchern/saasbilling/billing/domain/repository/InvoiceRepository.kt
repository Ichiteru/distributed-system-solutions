package com.ilchern.saasbilling.billing.domain.repository

import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.InvoiceType
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import java.time.Instant

interface InvoiceRepository {

  fun save(invoice: Invoice): Invoice

  fun findById(invoiceId: InvoiceId): Invoice?

  fun findBySubscriptionPeriod(
    subscriptionId: SubscriptionId,
    invoiceType: InvoiceType,
    periodStart: Instant,
    periodEnd: Instant,
  ): Invoice?
}
