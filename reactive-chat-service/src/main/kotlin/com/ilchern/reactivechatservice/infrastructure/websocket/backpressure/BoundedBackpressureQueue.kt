package com.ilchern.reactivechatservice.infrastructure.websocket.backpressure

import com.ilchern.reactivechatservice.infrastructure.metrics.OutboundBufferMetrics
import java.util.AbstractQueue
import java.util.ArrayDeque

class BoundedBackpressureQueue(
  private val capacity: Int,
  private val backpressurePolicy: BackpressurePolicy,
  private val outboundBufferMetrics: OutboundBufferMetrics,
) : AbstractQueue<OutboundMessage>() {

  private val queue = ArrayDeque<OutboundMessage>(capacity)

  override fun offer(element: OutboundMessage): Boolean = synchronized(queue) {
    if (queue.size < capacity) {
      queue.addLast(element)
      outboundBufferMetrics.incrementBufferSize()
      return true
    }

    return when (backpressurePolicy.onOverflow(element, queue)) {
      BackpressureDecision.ACCEPT -> {
        queue.addLast(element)
        outboundBufferMetrics.incrementBufferSize()
        true
      }

      BackpressureDecision.DROP_EPHEMERAL -> dropEphemeralAndContinue(element)
      BackpressureDecision.REJECT_CRITICAL -> false
    }
  }

  override fun poll(): OutboundMessage? = synchronized(queue) {
    val polled = queue.pollFirst()
    if (polled != null) {
      outboundBufferMetrics.decrementBufferSize()
    }
    polled
  }

  override fun peek(): OutboundMessage? = synchronized(queue) {
    queue.peekFirst()
  }

  override val size: Int
    get() = synchronized(queue) { queue.size }

  override fun iterator(): MutableIterator<OutboundMessage> = synchronized(queue) {
    ArrayList(queue).iterator()
  }

  fun clearAndRecordDroppedItems() = synchronized(queue) {
    val clearedItems = queue.size
    queue.clear()
    outboundBufferMetrics.decrementBufferSize(clearedItems)
  }

  private fun dropEphemeralAndContinue(incoming: OutboundMessage): Boolean {
    if (incoming.priority == OutboundMessagePriority.EPHEMERAL) {
      outboundBufferMetrics.recordEventDropped(incoming.priority)
      return true
    }

    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      if (iterator.next().priority == OutboundMessagePriority.EPHEMERAL) {
        iterator.remove()
        outboundBufferMetrics.recordEventDropped(OutboundMessagePriority.EPHEMERAL)
        queue.addLast(incoming)
        return true
      }
    }

    return false
  }
}
