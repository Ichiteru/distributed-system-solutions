1. Привести public contract к архитектурному документу.
   - Добавить единый event envelope для WebSocket и Redis: `eventId`, `eventType`, `correlationId`, `chatId`, `senderId`, `timestamp`, `payload`.
   - Поддержать inbound события `chat.message.created`, `chat.typing.started`, `chat.typing.stopped`.
   - Поддержать outbound события `chat.message.accepted`, `chat.message.created`, `chat.message.delivered`, `chat.message.rejected`, `error`.
   - Добавить явный error payload для throttling/backpressure с кодом `TOO_MANY_MESSAGES` и `httpStatus=429`.
   - Начать читать и валидировать `role` в `/ws/chat?userId=&chatId=&role=...`.

2. Реализовать bounded outbound queue и backpressure policy.
   - Заменить текущий unbounded `Sinks.many().unicast().onBackpressureBuffer()` на bounded per-session queue с конфигом `chat.outbound.buffer-size`.
   - Ввести `BackpressurePolicy` с решениями `ACCEPT`, `DROP_EPHEMERAL`, `REJECT_CRITICAL`.
   - Для `typing` и прочих ephemeral событий дропать первыми и считать дропы в метриках.
   - Для `chat.message` делать fail-fast reject без silent drop.
   - Сохранить текущий async reject path через `chat.message.rejected` для remote overflow.

3. Доработать message flow до целевой модели из architecture doc.
   - После успешного persistence делать локальный dispatch активным получателям текущего инстанса.
   - После локального dispatch публиковать `chat.message.created` в Redis для остальных инстансов.
   - Оставить `chat.message.delivered` как событие успешной постановки в outbound queue, а не client-level ack.
   - Явно разделить rate-limit reject и backpressure reject во внутренней логике, логах и метриках.

4. Реализовать reconnect/history replay.
   - Добавить query-методы в Mongo repository для загрузки последних сообщений по `chatId`.
   - Ввести конфиг `chat.history.reconnect-limit`.
   - При новом подключении отдавать recent history из Mongo активной сессии.
   - Не реплеить ephemeral события при reconnect.

5. Довести rate limiter до production-friendly состояния.
   - Добавить теги или унифицированные имена метрик для `allowed/rejected/backend_error`.
   - Добавить логирование `userId`, `decision`, `remainingTokens`, `retryAfterMillis`.
   - Обновить архитектурный документ: используется Redis Lua token bucket, а не Bucket4j.
   - Проверить и задокументировать fail-closed поведение при ошибке Redis.

6. Закрыть базовую observability.
   - Добавить `chat_ws_sessions_active`.
   - Добавить `chat_outbound_buffer_size`.
   - Добавить `chat_outbound_events_dropped_total`.
   - Добавить `chat_messages_rejected_total` и `chat_messages_reject_rate`.
   - Добавить `chat_delivery_latency_seconds`.
   - Добавить `chat_redis_events_published_total` и `chat_redis_events_consumed_total`.
   - Подготовить минимальный dashboard или хотя бы список Prometheus queries для демо.

7. Unit tests.
   - Backpressure policy: drop ephemeral first, reject critical on overflow.
   - Rate limiter: allow/reject/fail-closed.
   - Message flow: no save/publish on reject, `accepted` only after successful persistence.
   - Delivery semantics: `delivered` только после постановки в outbound queue.

8. Integration tests.
   - Mongo persistence before Redis publish.
   - Redis Pub/Sub delivery между двумя инстансами.
   - Async remote overflow rejection от instance B обратно sender-у на instance A.
   - Reconnect с догрузкой истории из Mongo.

9. Load tests.
   - Burst scenario без OOM и с контролируемой деградацией.
   - Slow consumer scenario для проверки overflow outbound queue.
   - Проверка SLA по latency (`p95 < 250 ms`) на тестовом стенде.
   - Снять и оформить метрики “до/после” для портфолио.

10. Финальная документация и acceptance artifacts.
   - Обновить `reactive-chat-service-architecture.md` под фактическую реализацию.
   - Зафиксировать naming convention Redis channels и финальный WebSocket contract.
   - Добавить раздел `Trade-offs`: почему Redis Pub/Sub, ограничения Pub/Sub, план эволюции.
   - Подготовить краткий лог-отчет или runbook, как запускать multi-instance demo и смотреть метрики.
