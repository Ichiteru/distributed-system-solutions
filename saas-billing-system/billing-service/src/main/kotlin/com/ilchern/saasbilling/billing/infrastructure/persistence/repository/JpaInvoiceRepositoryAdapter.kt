package com.ilchern.saasbilling.billing.infrastructure.persistence.repository

import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.InvoiceType
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.repository.InvoiceRepository
import com.ilchern.saasbilling.billing.infrastructure.persistence.mapper.InvoicePersistenceMapper
import java.time.Instant
import org.springframework.stereotype.Repository

@Repository
class JpaInvoiceRepositoryAdapter(
  private val invoiceJpaRepository: InvoiceJpaRepository,
  private val invoicePersistenceMapper: InvoicePersistenceMapper,
) : InvoiceRepository {

  override fun save(invoice: Invoice): Invoice {
    val entity = invoiceJpaRepository.findDetailedById(invoice.id.value)
      ?.also { entity -> invoicePersistenceMapper.copyToEntity(invoice, entity) }
      ?: invoicePersistenceMapper.newEntity(invoice)

    val savedEntity = invoiceJpaRepository.save(entity)
    return invoicePersistenceMapper.toDomain(savedEntity)
  }

  override fun findById(invoiceId: InvoiceId): Invoice? =
    invoiceJpaRepository.findDetailedById(invoiceId.value)
      ?.let(invoicePersistenceMapper::toDomain)

  override fun findBySubscriptionPeriod(
    subscriptionId: SubscriptionId,
    invoiceType: InvoiceType,
    periodStart: Instant,
    periodEnd: Instant,
  ): Invoice? =
    invoiceJpaRepository.findBySubscriptionIdAndInvoiceTypeAndPeriodStartAndPeriodEnd(
      subscriptionId = subscriptionId.value,
      invoiceType = invoiceType.name,
      periodStart = periodStart,
      periodEnd = periodEnd,
    )?.let(invoicePersistenceMapper::toDomain)
}
