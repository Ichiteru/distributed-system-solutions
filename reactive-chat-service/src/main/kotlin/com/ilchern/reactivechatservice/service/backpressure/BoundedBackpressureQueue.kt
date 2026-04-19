package com.ilchern.reactivechatservice.service.backpressure

import com.ilchern.reactivechatservice.service.ChatMetrics
import java.util.AbstractQueue
import java.util.ArrayDeque

class BoundedBackpressureQueue(
  private val capacity: Int,
  private val backpressurePolicy: BackpressurePolicy,
  private val chatMetrics: ChatMetrics,
) : AbstractQueue<OutboundMessage>() {

  private val queue = ArrayDeque<OutboundMessage>(capacity)

  override fun offer(element: OutboundMessage): Boolean = synchronized(queue) {
    if (queue.size < capacity) {
      queue.addLast(element)
      chatMetrics.incrementOutboundBufferSize()
      return true
    }

    return when (backpressurePolicy.onOverflow(element, queue)) {
      BackpressureDecision.ACCEPT -> {
        queue.addLast(element)
        chatMetrics.incrementOutboundBufferSize()
        true
      }

      BackpressureDecision.DROP_EPHEMERAL -> dropEphemeralAndContinue(element)
      BackpressureDecision.REJECT_CRITICAL -> false
    }
  }

  override fun poll(): OutboundMessage? = synchronized(queue) {
    val polled = queue.pollFirst()
    if (polled != null) {
      chatMetrics.decrementOutboundBufferSize()
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
    chatMetrics.decrementOutboundBufferSize(clearedItems)
  }

  private fun dropEphemeralAndContinue(incoming: OutboundMessage): Boolean {
    if (incoming.priority == OutboundMessagePriority.EPHEMERAL) {
      chatMetrics.recordOutboundEventDropped(incoming.priority)
      return true
    }

    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      if (iterator.next().priority == OutboundMessagePriority.EPHEMERAL) {
        iterator.remove()
        chatMetrics.recordOutboundEventDropped(OutboundMessagePriority.EPHEMERAL)
        queue.addLast(incoming)
        return true
      }
    }

    return false
  }
}
