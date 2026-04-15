package com.ilchern.reactivechatservice.service

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

interface SessionRegistry {
  fun register(session: WebSocketSession): Sinks.Many<String>
  fun remove(sessionId: String): RegisteredWebSocketSession?
  fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession>
}

@Service
class InMemorySessionRegistry : SessionRegistry {

  private val sessionsById = ConcurrentHashMap<String, RegisteredWebSocketSession>()
  private val sessionIdsByChatId = ConcurrentHashMap<String, MutableSet<String>>()

  override fun register(session: WebSocketSession) : Sinks.Many<String> {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
    val chatId = queryParams.getFirst("chatId") ?: error("CHAT ID NOT FOUND")
    val userId = queryParams.getFirst("userId") ?: error("CHAT ID NOT FOUND")

    val registeredSession = RegisteredWebSocketSession(
      sessionId = session.id,
      userId = queryParams.getFirst("userId") ?: error("USER ID NOT FOUND"),
      chatId = chatId,
      outboundSink = Sinks.many().unicast().onBackpressureBuffer()
    )

    sessionsById[registeredSession.sessionId] = registeredSession
    sessionIdsByChatId.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(registeredSession.sessionId)
    log.info("[{}:{}] register sessionId={}", userId, chatId, registeredSession.sessionId)

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
    removed.outboundSink.tryEmitComplete()
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
  val outboundSink: Sinks.Many<String>,
)
