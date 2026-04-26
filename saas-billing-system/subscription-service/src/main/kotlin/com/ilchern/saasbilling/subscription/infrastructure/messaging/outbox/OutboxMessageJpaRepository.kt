package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable

interface OutboxMessageJpaRepository : JpaRepository<OutboxMessageEntity, UUID>
{
  fun findAllByPublishedFalseOrderByOccurredAtAscIdAsc(pageable: Pageable): List<OutboxMessageEntity>
}
