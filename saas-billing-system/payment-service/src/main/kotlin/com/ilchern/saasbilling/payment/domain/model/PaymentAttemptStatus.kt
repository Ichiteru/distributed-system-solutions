package com.ilchern.saasbilling.payment.domain.model

enum class PaymentAttemptStatus {
  CREATED,
  SUBMITTED,
  SUCCEEDED,
  FAILED,
  TIMED_OUT,
}
