package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.repository

import com.ilchern.saasbilling.orchestrator.infrastructure.persistence.entity.BillingSagaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface BillingSagaJpaRepository : JpaRepository<BillingSagaEntity, UUID> {

  fun findBySagaTypeAndBusinessKey(
    sagaType: String,
    businessKey: String,
  ): BillingSagaEntity?
}
