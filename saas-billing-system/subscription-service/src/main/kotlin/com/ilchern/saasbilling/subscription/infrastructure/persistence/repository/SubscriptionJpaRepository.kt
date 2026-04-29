package com.ilchern.saasbilling.subscription.infrastructure.persistence.repository

import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.SubscriptionEntity
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionJpaRepository : JpaRepository<SubscriptionEntity, UUID> {

  @EntityGraph(attributePaths = ["pendingSubscriptionChange", "historyEntries"])
  fun findDetailedById(id: UUID): SubscriptionEntity?
}
