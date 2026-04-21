package com.ilchern.reactivechatservice.service

import com.ilchern.reactivechatservice.config.properties.OutboundBufferProperties
import com.ilchern.reactivechatservice.service.backpressure.BackpressurePolicy
import com.ilchern.reactivechatservice.service.backpressure.BoundedBackpressureQueue
import com.ilchern.reactivechatservice.service.backpressure.OutboundMessage
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

interface SessionRegistry {
  fun register(session: WebSocketSession): Sinks.Many<OutboundMessage>
  fun remove(sessionId: String): RegisteredWebSocketSession?
  fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession>
}

@Service
class InMemorySessionRegistry(
  private val sessionEmitService: SessionEmitService,
  private val backpressurePolicy: BackpressurePolicy,
  private val outboundBufferProperties: OutboundBufferProperties,
  private val chatMetrics: ChatMetrics,
) : SessionRegistry {

  private val sessionsById = ConcurrentHashMap<String, RegisteredWebSocketSession>()
  private val sessionIdsByChatId = ConcurrentHashMap<String, MutableSet<String>>()

  override fun register(session: WebSocketSession) : Sinks.Many<OutboundMessage> {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT ID NOT FOUND")
    val userId = queryParams.getFirst("userId") ?: error("CHAT ID NOT FOUND")
    val workerIndex = sessionEmitService.resolveWorkerIndex(session.id)

    val outboundQueue = BoundedBackpressureQueue(
      capacity = outboundBufferProperties.bufferSize,
      backpressurePolicy = backpressurePolicy,
      chatMetrics = chatMetrics,
    )

    val registeredSession = RegisteredWebSocketSession(
      sessionId = session.id,
      userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND"),
      chatId = chatId,
      outboundSink = Sinks.many().unicast().onBackpressureBuffer(outboundQueue),
      outboundQueue = outboundQueue,
      workerIndex = workerIndex,
    )

    sessionsById[registeredSession.sessionId] = registeredSession
    sessionIdsByChatId.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(registeredSession.sessionId)
    chatMetrics.recordSessionRegistered()
    log.info("[{}:{}] register sessionId={}, workerIndex={}", userId, chatId, registeredSession.sessionId, workerIndex)

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
    chatMetrics.recordSessionRemoved()
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
  val outboundSink: Sinks.Many<OutboundMessage>,
  val outboundQueue: BoundedBackpressureQueue,
  val workerIndex: Int,
)
