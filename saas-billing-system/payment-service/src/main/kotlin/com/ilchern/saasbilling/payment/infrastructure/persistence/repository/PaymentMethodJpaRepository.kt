package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.PaymentMethodEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentMethodJpaRepository : JpaRepository<PaymentMethodEntity, UUID> {
  fun findByOrganizationIdAndToken(
    organizationId: String,
    token: String,
  ): PaymentMethodEntity?
}
