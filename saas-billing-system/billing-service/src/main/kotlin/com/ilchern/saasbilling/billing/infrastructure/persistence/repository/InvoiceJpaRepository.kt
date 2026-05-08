package com.ilchern.saasbilling.billing.infrastructure.persistence.repository

import com.ilchern.saasbilling.billing.infrastructure.persistence.entity.InvoiceEntity
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface InvoiceJpaRepository : JpaRepository<InvoiceEntity, UUID> {

  @EntityGraph(attributePaths = ["lines"])
  fun findDetailedById(id: UUID): InvoiceEntity?

  @EntityGraph(attributePaths = ["lines"])
  fun findBySubscriptionIdAndInvoiceTypeAndPeriodStartAndPeriodEnd(
    subscriptionId: UUID,
    invoiceType: String,
    periodStart: Instant,
    periodEnd: Instant,
  ): InvoiceEntity?
}
