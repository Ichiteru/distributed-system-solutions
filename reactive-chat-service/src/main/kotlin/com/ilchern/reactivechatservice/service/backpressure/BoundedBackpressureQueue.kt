package com.ilchern.reactivechatservice.service.backpressure

import io.micrometer.core.instrument.MeterRegistry
import java.util.AbstractQueue
import java.util.ArrayDeque

class BoundedBackpressureQueue(
  private val capacity: Int,
  private val backpressurePolicy: BackpressurePolicy,
  meterRegistry: MeterRegistry,
) : AbstractQueue<OutboundMessage>() {

  private val queue = ArrayDeque<OutboundMessage>(capacity)
  private val droppedEphemeralCounter = meterRegistry.counter("chat_outbound_events_dropped_total")

  override fun offer(element: OutboundMessage): Boolean = synchronized(queue) {
    if (queue.size < capacity) {
      queue.addLast(element)
      return true
    }

    return when (backpressurePolicy.onOverflow(element, queue)) {
      BackpressureDecision.ACCEPT -> {
        queue.addLast(element)
        true
      }

      BackpressureDecision.DROP_EPHEMERAL -> dropEphemeralAndContinue(element)
      BackpressureDecision.REJECT_CRITICAL -> false
    }
  }

  override fun poll(): OutboundMessage? = synchronized(queue) {
    queue.pollFirst()
  }

  override fun peek(): OutboundMessage? = synchronized(queue) {
    queue.peekFirst()
  }

  override val size: Int
    get() = synchronized(queue) { queue.size }

  override fun iterator(): MutableIterator<OutboundMessage> = synchronized(queue) {
    ArrayList(queue).iterator()
  }

  private fun dropEphemeralAndContinue(incoming: OutboundMessage): Boolean {
    if (incoming.priority == OutboundMessagePriority.EPHEMERAL) {
      droppedEphemeralCounter.increment()
      return true
    }

    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      if (iterator.next().priority == OutboundMessagePriority.EPHEMERAL) {
        iterator.remove()
        droppedEphemeralCounter.increment()
        queue.addLast(incoming)
        return true
      }
    }

    return false
  }
}
