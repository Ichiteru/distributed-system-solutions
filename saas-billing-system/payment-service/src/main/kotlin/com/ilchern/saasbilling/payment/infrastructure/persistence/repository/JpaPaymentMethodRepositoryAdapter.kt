package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethod
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.repository.PaymentMethodRepository
import com.ilchern.saasbilling.payment.infrastructure.persistence.mapper.PaymentMethodPersistenceMapper
import java.time.Instant
import org.springframework.stereotype.Repository

@Repository
class JpaPaymentMethodRepositoryAdapter(
  private val paymentMethodJpaRepository: PaymentMethodJpaRepository,
  private val paymentMethodPersistenceMapper: PaymentMethodPersistenceMapper,
) : PaymentMethodRepository {

  override fun saveReference(
    organizationId: OrganizationId,
    token: PaymentMethodToken,
    observedAt: Instant,
  ): PaymentMethod {
    val paymentMethod = paymentMethodJpaRepository.findByOrganizationIdAndToken(
      organizationId = organizationId.value,
      token = token.value,
    )?.let { existing ->
      val domain = paymentMethodPersistenceMapper.toDomain(existing)
      domain.touch(observedAt)
      paymentMethodPersistenceMapper.copyToEntity(domain, existing)
      existing
    } ?: paymentMethodPersistenceMapper.newEntity(
      PaymentMethod.record(
        organizationId = organizationId,
        token = token,
        now = observedAt,
      ),
    )

    return paymentMethodPersistenceMapper.toDomain(paymentMethodJpaRepository.save(paymentMethod))
  }
}
