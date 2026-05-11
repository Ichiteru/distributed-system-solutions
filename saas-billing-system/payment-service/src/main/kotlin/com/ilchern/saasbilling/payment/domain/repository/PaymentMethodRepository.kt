package com.ilchern.saasbilling.payment.domain.repository

import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethod
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import java.time.Instant

interface PaymentMethodRepository {
  fun saveReference(
    organizationId: OrganizationId,
    token: PaymentMethodToken,
    observedAt: Instant,
  ): PaymentMethod
}
