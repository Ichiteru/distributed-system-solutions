package com.ilchern.reactivechatservice.service

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

interface SessionRegistry {
  fun register(userId: String, chatId: String, sessionId: String, outboundSink: Sinks.Many<String>)
  fun remove(sessionId: String): RegisteredWebSocketSession?
  fun getSessionsByChatId(chatId: String): List<RegisteredWebSocketSession>
}

@Service
class InMemorySessionRegistry : SessionRegistry {

  private val sessionsById = ConcurrentHashMap<String, RegisteredWebSocketSession>()
  private val sessionIdsByChatId = ConcurrentHashMap<String, MutableSet<String>>()

  override fun register(userId: String, chatId: String, sessionId: String, outboundSink: Sinks.Many<String>) {
    val session = RegisteredWebSocketSession(
      sessionId = sessionId,
      userId = userId,
      chatId = chatId,
      outboundSink = outboundSink,
    )

    sessionsById[sessionId] = session
    sessionIdsByChatId.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    log.info("[{}:{}] register sessionId={}", userId, chatId, sessionId)
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
