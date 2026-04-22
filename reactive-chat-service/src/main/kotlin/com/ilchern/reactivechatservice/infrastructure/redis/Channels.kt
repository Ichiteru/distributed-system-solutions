package com.ilchern.reactivechatservice.infrastructure.redis

object Channels {

  /** Отправителю - сервис принял сообщение и сохранил в хранилище */
  const val CHAT_MESSAGE_ACCEPTED = "chat.message.accepted"
  /** Получателю - сообщение от  отправителя сохранено в хранилище */
  const val CHAT_MESSAGE_CREATED = "chat.message.created"
  /** Отправителю - сообщение отправлено получателю(на вебсокет) */
  const val CHAT_MESSAGE_DELIVERED = "chat.message.delivered"
  /** Отправителю - ошибка доставки получателю(на вебсокет) */
  const val CHAT_MESSAGE_REJECTED = "chat.message.rejected"

}