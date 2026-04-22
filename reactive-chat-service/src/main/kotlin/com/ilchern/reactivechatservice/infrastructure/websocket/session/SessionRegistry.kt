package com.ilchern.reactivechatservice.infrastructure.websocket.session

import com.ilchern.reactivechatservice.config.properties.OutboundBufferProperties
import com.ilchern.reactivechatservice.infrastructure.metrics.OutboundBufferMetrics
import com.ilchern.reactivechatservice.infrastructure.metrics.WebSocketSessionMetrics
import com.ilchern.reactivechatservice.model.dto.ChatParticipantRole
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.BackpressurePolicy
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.BoundedBackpressureQueue
import com.ilchern.reactivechatservice.infrastructure.websocket.backpressure.OutboundMessage
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

interface SessionRegistry {
  fun register(request: SessionRegistrationRequest): Sinks.Many<OutboundMessage>
  fun remove(sessionId: String): RegisteredWebSocketSession?
  fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession>
}

@Service
class InMemorySessionRegistry(
  private val sessionEmitService: SessionEmitService,
  private val backpressurePolicy: BackpressurePolicy,
  private val outboundBufferProperties: OutboundBufferProperties,
  private val outboundBufferMetrics: OutboundBufferMetrics,
  private val webSocketSessionMetrics: WebSocketSessionMetrics,
) : SessionRegistry {

  private val sessionsById = ConcurrentHashMap<String, RegisteredWebSocketSession>()
  private val sessionIdsByChatId = ConcurrentHashMap<String, MutableSet<String>>()

  override fun register(request: SessionRegistrationRequest): Sinks.Many<OutboundMessage> {
    val workerIndex = sessionEmitService.resolveWorkerIndex(request.sessionId)
    val outboundQueue = BoundedBackpressureQueue(
      capacity = outboundBufferProperties.bufferSize,
      backpressurePolicy = backpressurePolicy,
      outboundBufferMetrics = outboundBufferMetrics,
    )

    val registeredSession = RegisteredWebSocketSession(
      sessionId = request.sessionId,
      userId = request.userId,
      chatId = request.chatId,
      role = request.role,
      outboundSink = Sinks.many().unicast().onBackpressureBuffer(outboundQueue),
      outboundQueue = outboundQueue,
      workerIndex = workerIndex,
    )

    sessionsById[registeredSession.sessionId] = registeredSession
    sessionIdsByChatId.computeIfAbsent(request.chatId) { ConcurrentHashMap.newKeySet() }.add(registeredSession.sessionId)
    webSocketSessionMetrics.recordRegistered()
    log.info(
      "[{}:{}] register sessionId={}, role={}, workerIndex={}",
      request.userId,
      request.chatId,
      registeredSession.sessionId,
      request.role,
      workerIndex,
    )

    return registeredSession.outboundSink
  }

  override fun remove(sessionId: String): RegisteredWebSocketSession? {
    val removed = sessionsById.remove(sessionId) ?: return null
    sessionIdsByChatId[removed.chatId]?.let { ids ->
      ids.remove(sessionId)
      if (ids.isEmpty()) {
        sessionIdsByChatId.remove(removed.chatId, ids)
      }
    }
    removed.outboundQueue.clearAndRecordDroppedItems()
    webSocketSessionMetrics.recordRemoved()
    sessionEmitService.complete(removed)
    log.info("[{}:{}] remove sessionId={}", removed.userId, removed.chatId, sessionId)
    return removed
  }

  override fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession> {
    val sessionIds = sessionIdsByChatId[chatId].orEmpty().toList()
    return sessionIds.mapNotNull { sessionId -> sessionsById[sessionId] }
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}

data class RegisteredWebSocketSession(
  val sessionId: String,
  val userId: String,
  val chatId: String,
  val role: ChatParticipantRole,
  val outboundSink: Sinks.Many<OutboundMessage>,
  val outboundQueue: BoundedBackpressureQueue,
  val workerIndex: Int,
)

data class SessionRegistrationRequest(
  val sessionId: String,
  val userId: String,
  val chatId: String,
  val role: ChatParticipantRole,
)
