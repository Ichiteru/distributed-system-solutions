# Pet Projects Portfolio

Набор небольших, но содержательных pet-проектов для демонстрации навыков в распределённых системах, реактивной разработке и современном стекe **Java/Kotlin + Spring + Reactive + DB + Messaging**.

| № | Название | Проблема | Стек | Ключевой паттерн / навык |
|---|----------|----------|------|---------------------------|
| 1 | **Distributed Task Scheduler** | Горизонтально масштабируемый планировщик задач с дедупликацией и отказоустойчивостью | Kotlin, Spring Boot, Kafka, Redis, MySQL, Docker | Event-driven, distributed locking, horizontal scaling |
| 2 | **Reactive Chat Service** | Мгновенный обмен сообщениями с backpressure и WebSocket | Java/Kotlin, Spring WebFlux, Project Reactor, Redis (pub/sub), MongoDB | Reactive streams, backpressure, pub/sub |
| 3 | **Event-Sourced Inventory System** | Управление запасами с историей изменений через event sourcing | Kotlin, Spring Boot, Kafka, Hibernate/JPA, MySQL | Event sourcing, CQRS, data replay |
| 4 | **GraphQL Aggregator** | Унифицированный API для данных из разных микросервисов с кешированием | Kotlin, Spring Boot, Spring GraphQL, Project Reactor, Redis, MySQL/MongoDB | API aggregation, caching, reactive queries |
| 5 | **Distributed Rate Limiter** | Ограничение API-запросов для нескольких сервисов | Java/Kotlin, Spring Boot, Redis, Docker, Project Reactor | Distributed counter, reactive throttling |
| 6 | **Fault-Tolerant File Upload Service** | Надёжная загрузка файлов с поддержкой S3/Yandex Object Storage и retry | Kotlin, Spring Boot, AWS S3 / Yandex Cloud, Kafka/RabbitMQ, Docker | Resilient integration, idempotency, retry patterns |
| 7 | **Reactive Financial Stream Processor** | Потоковая обработка транзакций с мониторингом аномалий | Java/Kotlin, Project Reactor, Kafka Streams, Clickhouse, Spring Boot, Docker | Stream processing, anomaly detection, reactive pipelines |
| 8 | **Search & Analytics Playground** | Быстрый поиск и фильтрация больших данных с агрегациями | Kotlin, Elasticsearch, Spring Boot, MongoDB/MySQL, Docker | Full-text search, analytics, aggregation queries |
| 9 | **Feature-Flag Management Service** | Реактивная система управления фичами с кешированием и realtime updates | Kotlin, Spring WebFlux, Redis, Kafka, Docker | Feature toggles, reactive caching, event-driven updates |
| 10 | **Mini SaaS Billing System** | Обработка подписок и платежей с event-driven интеграциями | Java/Kotlin, Spring Boot, RabbitMQ/Kafka, MySQL/Redis, Docker, Project Reactor | Event-driven billing, idempotency, transactional outbox |
| 11 | **Reactive Notification Hub** | Асинхронная рассылка email/push/telegram уведомлений с retry | Kotlin, Spring WebFlux, RabbitMQ, Redis, Docker | Reactive queues, retry, backpressure management |
| 12 | **Microservice Health Dashboard** | Сбор и визуализация метрик нескольких сервисов в реальном времени | Kotlin, Spring Boot, Project Reactor, Kafka, Grafana, OpenTelemetry | Observability, distributed tracing, reactive monitoring |
| 13 | **Transactional Outbox Demo** | Гарантированная доставка событий между сервисами через БД | Java/Kotlin, Spring Boot, MySQL, Kafka, Docker | Outbox pattern, eventual consistency, transaction integration |
| 14 | **Reactive API Gateway** | Gateway с асинхронной маршрутизацией, rate-limiting и кешированием | Kotlin, Spring WebFlux, Redis, Project Reactor, Docker | API Gateway, reactive routing, distributed cache |
| 15 | **Clickhouse Analytics Sandbox** | Высокопроизводительная аналитика событий и логов | Kotlin, Spring Boot, Clickhouse, Kafka, Docker | Columnar storage, high-throughput analytics, streaming ingestion |

---

Каждый проект компактный, с чёткой целью и демонстрирует владение современным стеком и паттернами распределённых систем.  
