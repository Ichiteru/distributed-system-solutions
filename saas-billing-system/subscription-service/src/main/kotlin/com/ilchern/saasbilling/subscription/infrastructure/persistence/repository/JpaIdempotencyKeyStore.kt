package com.ilchern.saasbilling.subscription.infrastructure.persistence.repository

import com.ilchern.saasbilling.subscription.application.port.IdempotencyKeyStore
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.IdempotencyKeyEntity
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
class JpaIdempotencyKeyStore(
  private val idempotencyKeyJpaRepository: IdempotencyKeyJpaRepository,
  private val clock: Clock,
) : IdempotencyKeyStore {

  override fun findSubscriptionId(
    organizationId: OrganizationId,
    operation: String,
    idempotencyKey: String,
  ): SubscriptionId? =
    idempotencyKeyJpaRepository.findByOrganizationIdAndOperationAndIdempotencyKey(
      organizationId = organizationId.value,
      operation = operation,
      idempotencyKey = idempotencyKey,
    )?.let { SubscriptionId(it.subscriptionId) }

  override fun save(
    organizationId: OrganizationId,
    operation: String,
    idempotencyKey: String,
    subscriptionId: SubscriptionId,
  ) {
    val entity = IdempotencyKeyEntity(
      id = UUID.randomUUID(),
      organizationId = organizationId.value,
      operation = operation,
      idempotencyKey = idempotencyKey,
      subscriptionId = subscriptionId.value,
      createdAt = clock.instant(),
    )
    idempotencyKeyJpaRepository.save(entity)
  }
}
