package com.ilchern.saasbilling.payment.application.command

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import java.time.Instant

data class HandleProviderWebhookCommand(
  val providerEventId: String,
  val providerPaymentReference: ProviderPaymentReference,
  val type: String,
  val status: String,
  val invoiceId: InvoiceId,
  val amount: Money,
  val occurredAt: Instant,
  val failureCode: String?,
  val failureMessage: String?,
)
