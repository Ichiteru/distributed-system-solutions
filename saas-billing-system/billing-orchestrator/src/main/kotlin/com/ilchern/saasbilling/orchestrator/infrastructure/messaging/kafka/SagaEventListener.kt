package com.ilchern.saasbilling.orchestrator.infrastructure.messaging.kafka

import com.ilchern.saasbilling.orchestrator.application.handler.SagaEventHandler
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SagaEventListener(
  private val envelopeReader: OutboxMessageEnvelopeReader,
  private val sagaEventHandler: SagaEventHandler,
) {

  @KafkaListener(
    topics = [
      "\${orchestrator.kafka.topics.subscription-events}",
      "\${orchestrator.kafka.topics.billing-events}",
      "\${orchestrator.kafka.topics.payment-events}",
    ],
    groupId = "\${spring.application.name}",
    containerFactory = "orchestratorEventKafkaListenerContainerFactory",
  )
  fun onMessage(record: GenericRecord) {
    sagaEventHandler.handle(envelopeReader.read(record))
  }
}
