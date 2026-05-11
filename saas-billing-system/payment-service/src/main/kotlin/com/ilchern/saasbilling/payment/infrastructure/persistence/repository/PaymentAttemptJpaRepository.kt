package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.PaymentAttemptEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentAttemptJpaRepository : JpaRepository<PaymentAttemptEntity, UUID> {
  fun findFirstByInvoiceIdOrderByAttemptNumberDesc(invoiceId: UUID): PaymentAttemptEntity?
}
