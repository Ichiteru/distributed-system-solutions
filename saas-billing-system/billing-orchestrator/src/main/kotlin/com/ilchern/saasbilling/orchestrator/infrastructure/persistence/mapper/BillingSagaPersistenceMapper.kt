package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.mapper

import com.ilchern.saasbilling.orchestrator.domain.model.BillingSaga
import com.ilchern.saasbilling.orchestrator.infrastructure.persistence.entity.BillingSagaEntity
import org.springframework.stereotype.Component

@Component
class BillingSagaPersistenceMapper {

  fun toDomain(entity: BillingSagaEntity): BillingSaga =
    BillingSaga.restore(
      id = entity.id,
      sagaType = entity.sagaType,
      businessKey = entity.businessKey,
      status = entity.status,
      correlationId = entity.correlationId,
      metadata = entity.metadata,
      startedAt = entity.startedAt,
      updatedAt = entity.updatedAt,
      completedAt = entity.completedAt,
    )

  fun toEntity(saga: BillingSaga): BillingSagaEntity =
    BillingSagaEntity(
      id = saga.id,
      sagaType = saga.sagaType,
      businessKey = saga.businessKey,
      status = saga.status(),
      correlationId = saga.correlationId,
      metadata = saga.metadata(),
      startedAt = saga.startedAt,
      updatedAt = saga.updatedAt(),
      completedAt = saga.completedAt(),
    )
}
