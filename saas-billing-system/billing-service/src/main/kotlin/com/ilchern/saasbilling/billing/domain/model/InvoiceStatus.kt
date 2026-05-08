package com.ilchern.saasbilling.billing.domain.model

enum class InvoiceStatus {
  DRAFT,
  OPEN,
  PAYMENT_PENDING,
  PAID,
  FAILED,
  VOID,
}
