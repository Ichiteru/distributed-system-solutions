package com.ilchern.saasbilling.orchestrator.application.handler

import com.ilchern.saasbilling.messaging.inbox.InboxMessage
import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import com.ilchern.saasbilling.orchestrator.application.service.EnvelopePathExtractor
import com.ilchern.saasbilling.orchestrator.application.service.OutboxMessageEnvelope
import com.ilchern.saasbilling.orchestrator.application.service.SagaDecisionService
import com.ilchern.saasbilling.orchestrator.application.service.SagaFlowProperties
import com.ilchern.saasbilling.orchestrator.application.service.SagaFlowRegistry
import com.ilchern.saasbilling.orchestrator.domain.model.BillingSaga
import com.ilchern.saasbilling.orchestrator.domain.repository.BillingSagaRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class SagaEventHandler(
  private val clock: Clock,
  private val inboxMessageProcessor: InboxMessageProcessor,
  private val sagaFlowRegistry: SagaFlowRegistry,
  private val sagaDecisionService: SagaDecisionService,
  private val billingSagaRepository: BillingSagaRepository,
  private val pathExtractor: EnvelopePathExtractor,
) {

  fun handle(envelope: OutboxMessageEnvelope) {
    inboxMessageProcessor.process(
      message = InboxMessage(
        consumer = "billing-orchestrator.events",
        messageId = envelope.id,
        messageType = envelope.type,
        aggregateId = envelope.aggregateId,
        correlationId = optionalUuid(envelope.headers["correlationId"]),
        causationId = optionalUuid(envelope.headers["causationId"]),
        receivedAt = clock.instant(),
        payload = envelope.payload,
        headers = envelopeHeaders(envelope),
      ),
    ) {
      applyEvent(envelope)
    }
  }

  private fun applyEvent(envelope: OutboxMessageEnvelope) {
    val context = findOrStartSaga(envelope)
    val extractedContext = extractContext(context.definition, envelope)
    context.saga.mergeMetadata(extractedContext)

    if (context.started) {
      billingSagaRepository.save(context.saga)
      return
    }

    val decision = sagaDecisionService.decide(
      definition = context.definition,
      currentStatus = context.saga.status(),
      eventType = envelope.type,
    )
    context.saga.transitionTo(
      newStatus = decision.nextStatus,
      terminal = decision.terminal,
      occurredAt = clock.instant(),
    )
    billingSagaRepository.save(context.saga)
  }

  private fun findOrStartSaga(envelope: OutboxMessageEnvelope): SagaContext {
    sagaFlowRegistry.findByStartEvent(envelope.type)
      ?.let { entry ->
        val businessKey = businessKey(entry.definition, envelope)
        val existingSaga = billingSagaRepository.findBySagaTypeAndBusinessKey(entry.sagaType, businessKey)
        if (existingSaga != null) {
          return SagaContext(entry.sagaType, entry.definition, existingSaga, started = true)
        }

        return SagaContext(
          sagaType = entry.sagaType,
          definition = entry.definition,
          saga = BillingSaga.start(
            sagaType = entry.sagaType,
            businessKey = businessKey,
            initialStatus = entry.definition.initialStatus,
            correlationId = correlationId(envelope, businessKey),
            metadata = extractContext(entry.definition, envelope),
            startedAt = clock.instant(),
          ),
          started = true,
        )
      }

    val candidates = sagaFlowRegistry.findByEvent(envelope.type)
    require(candidates.isNotEmpty()) { "No saga flow handles event ${envelope.type}" }

    candidates.forEach { entry ->
      val businessKey = businessKey(entry.definition, envelope)
      val saga = billingSagaRepository.findBySagaTypeAndBusinessKey(entry.sagaType, businessKey)
      if (saga != null) {
        return SagaContext(entry.sagaType, entry.definition, saga, started = false)
      }
    }

    error("Saga was not found for event ${envelope.type}")
  }

  private fun businessKey(
    definition: SagaFlowProperties.SagaDefinition,
    envelope: OutboxMessageEnvelope,
  ): String {
    val path = requireNotNull(definition.businessKeyPaths[envelope.type]) {
      "Missing business key path for event ${envelope.type}"
    }
    return pathExtractor.requiredString(envelope, path)
  }

  private fun extractContext(
    definition: SagaFlowProperties.SagaDefinition,
    envelope: OutboxMessageEnvelope,
  ): Map<String, Any> =
    pathExtractor.extractContext(envelope, definition.context[envelope.type].orEmpty())

  private fun correlationId(
    envelope: OutboxMessageEnvelope,
    businessKey: String,
  ): UUID? =
    optionalUuid(envelope.headers["correlationId"]) ?: optionalUuid(businessKey)

  private fun envelopeHeaders(envelope: OutboxMessageEnvelope): Map<String, Any> =
    envelope.headers + mapOf(
      "id" to envelope.id.toString(),
      "type" to envelope.type,
      "aggregateid" to envelope.aggregateId,
      "aggregatetype" to envelope.aggregateType,
      "timestamp" to envelope.timestamp.toString(),
    )

  private fun optionalUuid(value: Any?): UUID? =
    value?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

  private data class SagaContext(
    val sagaType: String,
    val definition: SagaFlowProperties.SagaDefinition,
    val saga: BillingSaga,
    val started: Boolean,
  )
}
