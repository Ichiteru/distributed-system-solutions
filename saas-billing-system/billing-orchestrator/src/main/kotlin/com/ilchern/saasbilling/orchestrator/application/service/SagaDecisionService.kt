package com.ilchern.saasbilling.orchestrator.application.service

import org.springframework.stereotype.Component

@Component
class SagaDecisionService {

  fun decide(
    definition: SagaFlowProperties.SagaDefinition,
    currentStatus: String,
    eventType: String,
  ): SagaDecision {
    val transition = definition.transitions.firstOrNull {
      it.from == currentStatus && it.on == eventType
    } ?: error("No transition from $currentStatus on $eventType")

    return SagaDecision(
      nextStatus = transition.to,
      terminal = definition.terminalStatuses.contains(transition.to),
      emit = transition.emit,
    )
  }
}

data class SagaDecision(
  val nextStatus: String,
  val terminal: Boolean,
  val emit: SagaFlowProperties.SagaEmit?,
)
