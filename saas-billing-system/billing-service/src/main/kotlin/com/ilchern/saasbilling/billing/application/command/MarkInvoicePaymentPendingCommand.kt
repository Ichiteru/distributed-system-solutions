package com.ilchern.saasbilling.billing.application.command

import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import java.time.Instant
import java.util.UUID

data class MarkInvoicePaymentPendingCommand(
  val invoiceId: InvoiceId,
  val amount: Money,
  val failureCode: String?,
  val failureMessage: String?,
  val messageId: UUID,
  val correlationId: UUID?,
  val causationId: UUID?,
  val occurredAt: Instant,
)
