[x] 1. Привести public contract к архитектурному документу.
   - [x] Добавить единый event envelope для WebSocket и Redis: `eventId`, `eventType`, `correlationId`, `chatId`, `senderId`, `timestamp`, `payload`.
   - [x] Поддержать inbound события `chat.message.created`, `chat.typing.started`, `chat.typing.stopped`.
   - [x] Поддержать outbound события `chat.message.accepted`, `chat.message.created`, `chat.message.delivered`, `chat.message.rejected`, `error`.
   - [x] Добавить явный error payload для throttling/backpressure с кодом `TOO_MANY_MESSAGES` и `httpStatus=429`.
   - [x] Начать читать и валидировать `role` в `/ws/chat?userId=&chatId=&role=...`.

[x] 2. Реализовать bounded outbound queue и backpressure policy.
   - [x] Заменить текущий unbounded `Sinks.many().unicast().onBackpressureBuffer()` на bounded per-session queue с конфигом `chat.outbound.buffer-size`.
   - [x] Ввести `BackpressurePolicy` с решениями `ACCEPT`, `DROP_EPHEMERAL`, `REJECT_CRITICAL`.
   - [x] Для `typing` и прочих ephemeral событий дропать первыми и считать дропы в метриках.
   - [x] Для `chat.message` делать fail-fast reject без silent drop.
   - [x] Сохранить текущий async reject path через `chat.message.rejected` для remote overflow.

[x] 3. Доработать message flow до целевой модели из architecture doc.
   - [x] Оставить `chat.message.delivered` как событие успешной постановки в outbound queue, а не client-level ack.
   - [x] Явно разделить rate-limit reject и backpressure reject во внутренней логике, логах и метриках.

[x] 4. Реализовать reconnect/history replay.
   - [x] Добавить query-методы в Mongo repository для загрузки последних сообщений по `chatId`.
   - [x] Ввести конфиг `chat.history.reconnect-limit`.
   - [x] При новом подключении отдавать recent history из Mongo активной сессии.
   - [x] Не реплеить ephemeral события при reconnect.

[x] 5. Довести rate limiter до production-friendly состояния.
   - [x] Добавить теги или унифицированные имена метрик для `allowed/rejected/backend_error`.
   - [x] Добавить логирование `userId`, `decision`, `remainingTokens`, `retryAfterMillis`.
   - [x] Обновить архитектурный документ: используется Redis Lua token bucket, а не Bucket4j.
   - [x] Проверить и задокументировать fail-closed поведение при ошибке Redis.

[x] 6. Закрыть базовую observability.
   - [x] Добавить `chat_ws_sessions_active`.
   - [x] Добавить `chat_outbound_buffer_size`.
   - [x] Добавить `chat_outbound_events_dropped_total`.
   - [x] Добавить `chat_messages_rejected_total` и `chat_messages_reject_rate`.
   - [x] Добавить `chat_delivery_latency_seconds`.
   - [x] Добавить `chat_redis_events_published_total` и `chat_redis_events_consumed_total`.
   - [x] Подготовить минимальный dashboard или хотя бы список Prometheus queries для демо.

[ ] 7. Unit tests.
   - [ ] Backpressure policy: drop ephemeral first, reject critical on overflow.
   - [ ] Rate limiter: allow/reject/fail-closed.
   - [ ] Message flow: no save/publish on reject, `accepted` only after successful persistence.
   - [ ] Delivery semantics: `delivered` только после постановки в outbound queue.

[ ] 8. Integration tests.
   - [ ] Mongo persistence before Redis publish.
   - [ ] Redis Pub/Sub delivery между двумя инстансами.
   - [ ] Async remote overflow rejection от instance B обратно sender-у на instance A.
   - [ ] Reconnect с догрузкой истории из Mongo.

[ ] 9. Load tests.
   - [x] `ws-smoke`: 5-10 клиентов, базовый handshake, отправка пары сообщений, проверка базовой доставки.
   - [x] `ws-burst`: 100-500 виртуальных пользователей, burst отправки сообщений, проверка rate limiter и rejects.
   - [ ] `ws-pair-delivery`: sender/receiver пары, проверка delivery path и статусов `accepted/delivered/rejected`.
   - [ ] `ws-slow-consumer`: медленный receiver, заполнение outbound queue, проверка backpressure и drops/rejects.
   - [ ] `ws-reconnect`: disconnect/reconnect, проверка догрузки history из MongoDB.
   - [x] `ws-multi-chat`: много `chatId` и пользователей, проверка распределения нагрузки и изоляции чатов.
   - [ ] `ws-long-lived`: долгоживущие WebSocket-сессии, проверка памяти, GC и стабильности соединений.
   - [ ] Burst scenario без OOM и с контролируемой деградацией.
   - [ ] Slow consumer scenario для проверки overflow outbound queue.
   - [ ] Проверка SLA по latency (`p95 < 250 ms`) на тестовом стенде.
   - [ ] Снять и оформить метрики “до/после” для портфолио.

[x] 10. Docker и multi-instance deployment через `nginx`.
   - [x] Добавить `Dockerfile` для `reactive-chat-service`.
   - [x] Добавить `docker-compose` для MongoDB, Redis, `nginx` и `N` инстансов `reactive-chat-service`.
   - [x] Добавить `nginx.conf` с upstream для `reactive-chat-service-*` и поддержкой WebSocket upgrade.
   - [x] Вынести конфигурацию инстансов через env vars: порты, Mongo URI, Redis host/port, worker pool size.
   - [x] Подготовить локальный сценарий запуска нескольких инстансов через `nginx` для демонстрации Redis Pub/Sub delivery и reconnect/history replay.
   - [x] Проверить, что multi-instance запуск воспроизводим без ручных правок конфигов.

[ ] 11. Финальная документация и acceptance artifacts.
   - [x] Обновить `reactive-chat-service-architecture.md` под фактическую реализацию rate limiter и reconnect/history.
   - [ ] Зафиксировать naming convention Redis channels и финальный WebSocket contract.
   - [ ] Добавить раздел `Trade-offs`: почему Redis Pub/Sub, ограничения Pub/Sub, план эволюции.
   - [ ] Подготовить краткий лог-отчет или runbook, как запускать multi-instance demo и смотреть метрики.
