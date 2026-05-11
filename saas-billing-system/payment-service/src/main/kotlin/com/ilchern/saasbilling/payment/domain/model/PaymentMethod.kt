package com.ilchern.saasbilling.payment.domain.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class PaymentMethodId(val value: UUID)

class PaymentMethod private constructor(
  val id: PaymentMethodId,
  val organizationId: OrganizationId,
  val token: PaymentMethodToken,
  val createdAt: Instant,
  private var updatedAt: Instant,
) {

  fun updatedAt(): Instant = updatedAt

  fun touch(updatedAt: Instant) {
    this.updatedAt = updatedAt
  }

  companion object {
    fun record(
      organizationId: OrganizationId,
      token: PaymentMethodToken,
      now: Instant,
    ): PaymentMethod {
      require(token.value.isNotBlank()) { "Payment method token must not be blank" }
      return PaymentMethod(
        id = PaymentMethodId(UUID.randomUUID()),
        organizationId = organizationId,
        token = token,
        createdAt = now,
        updatedAt = now,
      )
    }

    fun restore(
      id: PaymentMethodId,
      organizationId: OrganizationId,
      token: PaymentMethodToken,
      createdAt: Instant,
      updatedAt: Instant,
    ): PaymentMethod =
      PaymentMethod(
        id = id,
        organizationId = organizationId,
        token = token,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
  }
}
