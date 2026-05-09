package com.ilchern.saasbilling.orchestrator.infrastructure.persistence.repository

import com.ilchern.saasbilling.orchestrator.domain.model.BillingSaga
import com.ilchern.saasbilling.orchestrator.domain.repository.BillingSagaRepository
import com.ilchern.saasbilling.orchestrator.infrastructure.persistence.mapper.BillingSagaPersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class JpaBillingSagaRepositoryAdapter(
  private val billingSagaJpaRepository: BillingSagaJpaRepository,
  private val mapper: BillingSagaPersistenceMapper,
) : BillingSagaRepository {

  override fun findBySagaTypeAndBusinessKey(
    sagaType: String,
    businessKey: String,
  ): BillingSaga? =
    billingSagaJpaRepository.findBySagaTypeAndBusinessKey(sagaType, businessKey)
      ?.let(mapper::toDomain)

  override fun save(saga: BillingSaga): BillingSaga =
    mapper.toDomain(billingSagaJpaRepository.save(mapper.toEntity(saga)))
}
