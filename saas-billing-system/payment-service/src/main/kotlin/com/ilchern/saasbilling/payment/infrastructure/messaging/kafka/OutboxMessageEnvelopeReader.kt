package com.ilchern.saasbilling.payment.infrastructure.messaging.kafka

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.springframework.stereotype.Component

@Component
class OutboxMessageEnvelopeReader(
  private val objectMapper: ObjectMapper,
) {

  fun read(record: GenericRecord): OutboxMessageEnvelope =
    OutboxMessageEnvelope(
      id = UUID.fromString(requiredString(record, "id")),
      type = requiredString(record, "type"),
      aggregateType = requiredString(record, "aggregatetype"),
      aggregateId = requiredString(record, "aggregateid"),
      timestamp = instant(record.get("timestamp")),
      headers = jsonMap(record.get("headers"), "headers"),
      payload = jsonMap(record.get("payload"), "payload"),
    )

  private fun requiredString(
    record: GenericRecord,
    field: String,
  ): String =
    requireNotNull(record.get(field)) { "Missing outbox envelope field $field" }.toString()

  private fun instant(value: Any?): Instant {
    val requiredValue = requireNotNull(value) { "Missing outbox envelope field timestamp" }
    return when (requiredValue) {
      is Instant -> requiredValue
      is LocalDateTime -> requiredValue.toInstant(ZoneOffset.UTC)
      is Number -> Instant.ofEpochMilli(requiredValue.toLong())
      else -> Instant.parse(requiredValue.toString())
    }
  }

  private fun jsonMap(
    value: Any?,
    field: String,
  ): Map<String, Any> {
    val json = requireNotNull(value) { "Missing outbox envelope field $field" }.toString()
    return objectMapper.readValue(json, MAP_TYPE_REFERENCE)
  }

  companion object {
    private val MAP_TYPE_REFERENCE = object : TypeReference<Map<String, Any>>() {}
  }
}
