package com.ilchern.saasbilling.orchestrator.domain.repository

import com.ilchern.saasbilling.orchestrator.domain.model.BillingSaga

interface BillingSagaRepository {

  fun findBySagaTypeAndBusinessKey(
    sagaType: String,
    businessKey: String,
  ): BillingSaga?

  fun save(saga: BillingSaga): BillingSaga
}
