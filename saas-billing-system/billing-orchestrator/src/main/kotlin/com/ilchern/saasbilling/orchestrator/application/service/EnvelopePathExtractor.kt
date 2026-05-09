package com.ilchern.saasbilling.orchestrator.application.service

import org.springframework.stereotype.Component

@Component
class EnvelopePathExtractor {

  fun requiredString(
    envelope: OutboxMessageEnvelope,
    path: String,
  ): String =
    requireNotNull(extract(envelope, path)) { "Missing envelope path $path for ${envelope.type}" }.toString()

  fun extractContext(
    envelope: OutboxMessageEnvelope,
    paths: Map<String, String>,
  ): Map<String, Any> =
    paths.mapNotNull { (field, path) ->
      extract(envelope, path)?.let { field to it }
    }.toMap()

  fun extract(
    envelope: OutboxMessageEnvelope,
    path: String,
  ): Any? {
    require(path.startsWith(ROOT_PREFIX)) { "Unsupported envelope path $path" }

    return when {
      path == "$.id" -> envelope.id.toString()
      path == "$.type" -> envelope.type
      path == "$.aggregateid" -> envelope.aggregateId
      path == "$.aggregatetype" -> envelope.aggregateType
      path == "$.timestamp" -> envelope.timestamp.toString()
      path.startsWith(PAYLOAD_PREFIX) -> envelope.payload[path.removePrefix(PAYLOAD_PREFIX)]
      path.startsWith(HEADERS_PREFIX) -> envelope.headers[path.removePrefix(HEADERS_PREFIX)]
      else -> error("Unsupported envelope path $path")
    }
  }

  companion object {
    private const val ROOT_PREFIX = "$."
    private const val PAYLOAD_PREFIX = "$.payload."
    private const val HEADERS_PREFIX = "$.headers."
  }
}
