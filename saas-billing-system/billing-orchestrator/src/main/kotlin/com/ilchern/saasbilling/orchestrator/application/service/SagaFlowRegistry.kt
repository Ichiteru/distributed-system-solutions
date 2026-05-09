package com.ilchern.saasbilling.orchestrator.application.service

import org.springframework.stereotype.Component

@Component
class SagaFlowRegistry(
  private val properties: SagaFlowProperties,
) {

  fun findByStartEvent(eventType: String): SagaDefinitionEntry? =
    properties.sagas.entries
      .firstOrNull { (_, definition) -> definition.startEvent == eventType }
      ?.let { (sagaType, definition) -> SagaDefinitionEntry(sagaType, definition) }

  fun findByEvent(eventType: String): List<SagaDefinitionEntry> =
    properties.sagas.entries
      .filter { (_, definition) ->
        definition.startEvent == eventType ||
          definition.transitions.any { transition -> transition.on == eventType }
      }
      .map { (sagaType, definition) -> SagaDefinitionEntry(sagaType, definition) }

  data class SagaDefinitionEntry(
    val sagaType: String,
    val definition: SagaFlowProperties.SagaDefinition,
  )
}
