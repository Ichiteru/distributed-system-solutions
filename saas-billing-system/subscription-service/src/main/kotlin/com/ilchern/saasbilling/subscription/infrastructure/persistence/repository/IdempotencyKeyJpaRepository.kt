package com.ilchern.saasbilling.subscription.infrastructure.persistence.repository

import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.IdempotencyKeyEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyKeyJpaRepository : JpaRepository<IdempotencyKeyEntity, UUID> {

  fun findByOrganizationIdAndOperationAndIdempotencyKey(
    organizationId: String,
    operation: String,
    idempotencyKey: String,
  ): IdempotencyKeyEntity?
}
