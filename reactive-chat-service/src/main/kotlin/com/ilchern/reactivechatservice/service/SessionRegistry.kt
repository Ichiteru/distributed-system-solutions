package com.ilchern.reactivechatservice.service

import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.PreDestroy

interface SessionRegistry {
  fun register(session: WebSocketSession): Sinks.Many<String>
  fun emit(sessionId: String, payload: String): Mono<Sinks.EmitResult>
  fun remove(sessionId: String): RegisteredWebSocketSession?
  fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession>
}

@Service
class InMemorySessionRegistry(
  @Value("\${chat.session-worker-pool-size:0}") configuredWorkerPoolSize: Int,
) : SessionRegistry {

  private val sessionsById = ConcurrentHashMap<String, RegisteredWebSocketSession>()
  private val sessionIdsByChatId = ConcurrentHashMap<String, MutableSet<String>>()
  private val workerPoolSize = configuredWorkerPoolSize
    .takeIf { it > 0 }
    ?: Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
  private val workers: Array<Scheduler> =
    Array(workerPoolSize) { index -> Schedulers.newSingle("session-outbound-$index") }

  override fun register(session: WebSocketSession) : Sinks.Many<String> {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT ID NOT FOUND")
    val userId = queryParams.getFirst("userId") ?: error("CHAT ID NOT FOUND")
    val workerIndex = Math.floorMod(session.id.hashCode(), workerPoolSize)

    val registeredSession = RegisteredWebSocketSession(
      sessionId = session.id,
      userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND"),
      chatId = chatId,
      outboundSink = Sinks.many().unicast().onBackpressureBuffer(),
      workerIndex = workerIndex,
    )

    sessionsById[registeredSession.sessionId] = registeredSession
    sessionIdsByChatId.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(registeredSession.sessionId)
    log.info("[{}:{}] register sessionId={}, workerIndex={}", userId, chatId, registeredSession.sessionId, workerIndex)

    return registeredSession.outboundSink
  }

  override fun emit(sessionId: String, payload: String): Mono<Sinks.EmitResult> {
    return Mono.justOrEmpty<RegisteredWebSocketSession>(sessionsById[sessionId])
      .flatMap { session ->
        Mono.create { sink ->
          workers[session.workerIndex].schedule {
            sink.success(session.outboundSink.tryEmitNext(payload))
          }
        }
      }
      .defaultIfEmpty(Sinks.EmitResult.FAIL_TERMINATED)
  }

  override fun remove(sessionId: String): RegisteredWebSocketSession? {
    val removed = sessionsById.remove(sessionId) ?: return null
    sessionIdsByChatId[removed.chatId]?.let { ids ->
      ids.remove(sessionId)
      if (ids.isEmpty()) {
        sessionIdsByChatId.remove(removed.chatId, ids)
      }
    }
    workers[removed.workerIndex].schedule {
      removed.outboundSink.tryEmitComplete()
    }
    log.info("[{}:{}] remove sessionId={}", removed.userId, removed.chatId, sessionId)
    return removed
  }

  override fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession> {
    val sessionIds = sessionIdsByChatId[chatId].orEmpty().toList()
    return sessionIds.mapNotNull { sessionId -> sessionsById[sessionId] }
  }

  @PreDestroy
  fun shutdown() {
    workers.forEach(Scheduler::dispose)
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}

data class RegisteredWebSocketSession(
  val sessionId: String,
  val userId: String,
  val chatId: String,
  val outboundSink: Sinks.Many<String>,
  val workerIndex: Int,
)
