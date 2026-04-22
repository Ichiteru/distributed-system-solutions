package com.ilchern.reactivechatservice.handler

import com.ilchern.reactivechatservice.model.dto.ChatParticipantRole
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import java.util.Locale

@Component
class ChatWebSocketConnectionContextFactory {

  fun create(session: WebSocketSession): ChatConnectionContext {
    val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams

    return ChatConnectionContext(
      sessionId = session.id,
      userId = queryParams.required(USER_ID_QUERY_PARAM),
      chatId = queryParams.required(CHAT_ID_QUERY_PARAM),
      role = queryParams.required(ROLE_QUERY_PARAM).toParticipantRole(),
    )
  }

  private fun MultiValueMap<String, String>.required(name: String): String {
    return getFirst(name) ?: error("Missing required WebSocket query parameter: $name")
  }

  private fun String.toParticipantRole(): ChatParticipantRole {
    return ChatParticipantRole.valueOf(uppercase(Locale.ROOT))
  }

  private companion object {
    private const val USER_ID_QUERY_PARAM = "userId"
    private const val CHAT_ID_QUERY_PARAM = "chatId"
    private const val ROLE_QUERY_PARAM = "role"
  }
}
