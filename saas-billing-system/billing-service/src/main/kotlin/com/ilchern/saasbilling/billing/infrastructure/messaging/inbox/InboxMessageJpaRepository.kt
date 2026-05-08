package com.ilchern.saasbilling.billing.infrastructure.messaging.inbox

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface InboxMessageJpaRepository : JpaRepository<InboxMessageEntity, UUID>
