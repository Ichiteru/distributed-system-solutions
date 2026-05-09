package com.ilchern.saasbilling.orchestrator.infrastructure.messaging.kafka

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.orchestrator.application.service.OutboxMessageEnvelope
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.springframework.stereotype.Component

@Component
class OutboxMessageEnvelopeReader(
  private val objectMapper: ObjectMapper,
) {

  fun read(record: GenericRecord): OutboxMessageEnvelope {
    val parsedRecord = genericRecordToMapRecursive(record)
    return OutboxMessageEnvelope(
      id = UUID.fromString(parsedRecord["id"].toString()) ,
      type = parsedRecord["type"].toString(),
      aggregateType = parsedRecord["aggregatetype"].toString(),
      aggregateId = parsedRecord["aggregateid"].toString(),
      timestamp = instant(parsedRecord["timestamp"]),
      headers = objectMapper.readValue(parsedRecord["headers"].toString(), MAP_TYPE_REFERENCE),
      payload = objectMapper.readValue(parsedRecord["payload"].toString(), MAP_TYPE_REFERENCE),
    )
  }

  fun genericRecordToMapRecursive(record: GenericRecord): Map<String, Any> {
    return record.schema.fields.associate { field ->
      val value = record.get(field.name())
      field.name() to when (value) {
        is GenericRecord -> genericRecordToMapRecursive(value)
        else -> value
      }
    }
  }


  private fun instant(value: Any?): Instant {
    val requiredValue = requireNotNull(value) { "Missing outbox envelope field timestamp" }
    return when (requiredValue) {
      is Instant -> requiredValue
      is LocalDateTime -> requiredValue.toInstant(ZoneOffset.UTC)
      is Number -> Instant.ofEpochMilli(requiredValue.toLong())
      else -> Instant.parse(requiredValue.toString())
    }
  }

  companion object {
    private val MAP_TYPE_REFERENCE = object : TypeReference<Map<String, Any>>() {}
  }
}
