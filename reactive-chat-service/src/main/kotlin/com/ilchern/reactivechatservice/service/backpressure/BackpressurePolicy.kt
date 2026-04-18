package com.ilchern.reactivechatservice.service.backpressure

import org.springframework.stereotype.Service

enum class BackpressureDecision {
  ACCEPT,
  DROP_EPHEMERAL,
  REJECT_CRITICAL,
}

enum class OutboundMessagePriority {
  CRITICAL,
  EPHEMERAL,
}

data class OutboundMessage(
  val payload: String,
  val priority: OutboundMessagePriority,
)

interface BackpressurePolicy {
  fun onOverflow(incoming: OutboundMessage, queued: Collection<OutboundMessage>): BackpressureDecision
}

@Service
class DefaultBackpressurePolicy : BackpressurePolicy {

  override fun onOverflow(
    incoming: OutboundMessage,
    queued: Collection<OutboundMessage>,
  ): BackpressureDecision {
    if (incoming.priority == OutboundMessagePriority.EPHEMERAL) {
      return BackpressureDecision.DROP_EPHEMERAL
    }

    return if (queued.any { it.priority == OutboundMessagePriority.EPHEMERAL }) {
      BackpressureDecision.DROP_EPHEMERAL
    } else {
      BackpressureDecision.REJECT_CRITICAL
    }
  }
}
