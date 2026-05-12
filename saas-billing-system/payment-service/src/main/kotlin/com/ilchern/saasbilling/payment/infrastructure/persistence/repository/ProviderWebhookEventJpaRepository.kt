package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.ProviderWebhookEventEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ProviderWebhookEventJpaRepository : JpaRepository<ProviderWebhookEventEntity, UUID> {
  fun existsByProviderEventId(providerEventId: String): Boolean
}
