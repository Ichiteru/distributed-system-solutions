package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.repository

import com.ilchern.saasbilling.orchestrator.infrastructure.persistence.entity.CommandOutboxEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CommandOutboxJpaRepository : JpaRepository<CommandOutboxEntity, UUID>
