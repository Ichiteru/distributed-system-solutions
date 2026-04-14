package com.ilchern.reactivechatservice.service

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

interface WebSocketSessionRegistry {

  fun get(userId: String, chatId: String): WebSocketSession?
  fun put(userId: String, chatId: String, session: WebSocketSession): WebSocketSession
  fun remove(userId: String, chatId: String): WebSocketSession?
}

@Service
class InMemoryWebSocketSessionRegistry : WebSocketSessionRegistry {

  private val registry = ConcurrentHashMap<String, WebSocketSession>()

  override fun get(userId: String, chatId: String) : WebSocketSession? {
    return registry["$userId:$chatId"]
  }

  override fun put(userId: String, chatId: String, session: WebSocketSession): WebSocketSession {
    log.info("[$userId$chatId] put session to registry")
    registry["$userId:$chatId"] = session
    return session
  }

  override fun remove(userId: String, chatId: String): WebSocketSession? {
    log.info("[$userId$chatId] remove session from registry")
    return registry.remove("$userId:$chatId")
  }

  private companion object {
    private val log = LogManager.getLogger()
  }
}