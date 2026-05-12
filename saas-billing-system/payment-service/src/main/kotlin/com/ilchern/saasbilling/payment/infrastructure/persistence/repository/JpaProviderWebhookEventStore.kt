package com.ilchern.saasbilling.payment.infrastructure.persistence.repository

import com.ilchern.saasbilling.payment.application.port.ProviderWebhookEvent
import com.ilchern.saasbilling.payment.application.port.ProviderWebhookEventStore
import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.ProviderWebhookEventEntity
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class JpaProviderWebhookEventStore(
  private val providerWebhookEventJpaRepository: ProviderWebhookEventJpaRepository,
) : ProviderWebhookEventStore {

  override fun saveIfAbsent(event: ProviderWebhookEvent): Boolean =
    if (providerWebhookEventJpaRepository.existsByProviderEventId(event.providerEventId)) {
      false
    } else try {
      providerWebhookEventJpaRepository.saveAndFlush(
        ProviderWebhookEventEntity(
          id = UUID.randomUUID(),
          providerEventId = event.providerEventId,
          providerPaymentId = event.providerPaymentId,
          type = event.type,
          status = event.status,
          receivedAt = event.receivedAt,
        ),
      )
      true
    } catch (_: DataIntegrityViolationException) {
      false
    }
}
