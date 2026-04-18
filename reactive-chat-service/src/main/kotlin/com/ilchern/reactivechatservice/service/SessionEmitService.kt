package com.ilchern.reactivechatservice.service

import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

interface SessionEmitService {
  fun resolveWorkerIndex(sessionId: String): Int
  fun emit(session: RegisteredWebSocketSession, payload: String): Mono<Sinks.EmitResult>
  fun complete(session: RegisteredWebSocketSession)
}

@Service
class PartitionedSessionEmitService(
  private val sessionWorkers: Array<Scheduler>
) : SessionEmitService {

  override fun resolveWorkerIndex(sessionId: String): Int {
    return Math.floorMod(sessionId.hashCode(), sessionWorkers.size)
  }

  override fun emit(session: RegisteredWebSocketSession, payload: String): Mono<Sinks.EmitResult> {
    return Mono.create { sink ->
      sessionWorkers[session.workerIndex].schedule {
        sink.success(session.outboundSink.tryEmitNext(payload))
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
