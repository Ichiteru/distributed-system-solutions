package com.ilchern.reactivechatservice.infrastructure.websocket.session

import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessagePriority
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler

interface SessionWorkerResolver {
  fun workerIndexFor(sessionId: String): Int
}

interface SessionOutboundDispatcher {
  fun dispatch(
    session: RegisteredWebSocketSession,
    payload: String,
    priority: OutboundMessagePriority = OutboundMessagePriority.CRITICAL,
  ): Mono<Sinks.EmitResult>

  fun completeOutbound(session: RegisteredWebSocketSession)
}

@Service
class PartitionedSessionOutboundDispatcher(
  private val sessionWorkers: Array<Scheduler>,
) : SessionWorkerResolver, SessionOutboundDispatcher {

  override fun workerIndexFor(sessionId: String): Int {
    return Math.floorMod(sessionId.hashCode(), sessionWorkers.size)
  }

  override fun dispatch(
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

  override fun completeOutbound(session: RegisteredWebSocketSession) {
    sessionWorkers[session.workerIndex].schedule {
      session.outboundSink.tryEmitComplete()
    }
  }

  @PreDestroy
  fun shutdown() {
    sessionWorkers.forEach(Scheduler::dispose)
  }
}
