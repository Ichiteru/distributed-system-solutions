package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt
import com.ilchern.saasbilling.payment.domain.repository.PaymentAttemptRepository
import com.ilchern.saasbilling.payment.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class JpaPaymentAttemptRepositoryAdapter(
  private val paymentAttemptJpaRepository: PaymentAttemptJpaRepository,
  private val paymentAttemptPersistenceMapper: PaymentAttemptPersistenceMapper,
) : PaymentAttemptRepository {

  override fun save(paymentAttempt: PaymentAttempt): PaymentAttempt {
    val entity = paymentAttemptJpaRepository.findById(paymentAttempt.id.value)
      .map { existing ->
        paymentAttemptPersistenceMapper.copyToEntity(paymentAttempt, existing)
        existing
      }
      .orElseGet { paymentAttemptPersistenceMapper.newEntity(paymentAttempt) }

    return paymentAttemptPersistenceMapper.toDomain(paymentAttemptJpaRepository.save(entity))
  }

  override fun findLatestByInvoiceId(invoiceId: InvoiceId): PaymentAttempt? =
    paymentAttemptJpaRepository.findFirstByInvoiceIdOrderByAttemptNumberDesc(invoiceId.value)
      ?.let(paymentAttemptPersistenceMapper::toDomain)
}
