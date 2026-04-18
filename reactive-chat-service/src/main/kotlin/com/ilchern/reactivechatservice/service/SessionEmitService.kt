package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.service.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.service.backpressure.OutboundMessagePriority
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

interface SessionEmitService {
  fun resolveWorkerIndex(sessionId: String): Int
  fun emit(
    session: RegisteredWebSocketSession,
    payload: String,
    priority: OutboundMessagePriority = OutboundMessagePriority.CRITICAL,
  ): Mono<Sinks.EmitResult>
  fun complete(session: RegisteredWebSocketSession)
}

@Service
class PartitionedSessionEmitService(
  private val sessionWorkers: Array<Scheduler>
) : SessionEmitService {

  override fun resolveWorkerIndex(sessionId: String): Int {
    return Math.floorMod(sessionId.hashCode(), sessionWorkers.size)
  }

  override fun emit(
    session: RegisteredWebSocketSession,
    payload: String,
    priority: OutboundMessagePriority,
  ): Mono<Sinks.EmitResult> {
    return Mono.create { sink ->
      sessionWorkers[session.workerIndex].schedule {
        sink.success(session.outboundSink.tryEmitNext(OutboundMessage(payload, priority)))
      }
    }
  }

  override fun complete(session: RegisteredWebSocketSession) {
    sessionWorkers[session.workerIndex].schedule {
      session.outboundSink.tryEmitComplete()
    }
  }

  @PreDestroy
  fun shutdown() {
    sessionWorkers.forEach(Scheduler::dispose)
  }
}
