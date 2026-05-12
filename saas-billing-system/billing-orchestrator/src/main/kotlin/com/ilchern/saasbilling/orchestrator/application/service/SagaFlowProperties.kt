package com.ilchern.saasbilling.orchestrator.application.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "orchestrator")
data class SagaFlowProperties(
  val sagas: Map<String, SagaDefinition> = emptyMap(),
) {

  data class SagaDefinition(
    val startEvent: String,
    val initialStatus: String,
    val terminalStatuses: Set<String> = emptySet(),
    val businessKeyPath: String,
    val context: Map<String, Map<String, String>> = emptyMap(),
    val transitions: List<SagaTransition> = emptyList(),
  )

  data class SagaTransition(
    val from: String,
    val on: String,
    val to: String,
    val emit: SagaEmit? = null,
  )

  data class SagaEmit(
    val topic: String,
    val type: String,
    val schemaVersion: Int = 1,
    val aggregateType: String? = null,
    val aggregateIdPath: String? = null,
  )
}
