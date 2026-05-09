package com.ilchern.saasbilling.messaging.jpa.outbox

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxMessageJpaRepository : JpaRepository<OutboxMessageEntity, UUID>
