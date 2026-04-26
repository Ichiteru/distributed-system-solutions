package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxMessageJpaRepository : JpaRepository<OutboxMessageEntity, UUID>
