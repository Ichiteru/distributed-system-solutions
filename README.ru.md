# Distributed System Solutions

Portfolio-репозиторий с практическими backend-проектами про distributed systems, event-driven architecture, reactive programming, reliability patterns, observability и load testing.

## Проекты

| Проект | Описание | Stack |
| --- | --- | --- |
| [reactive-chat-service](reactive-chat-service/README.md) | Reactive low-latency WebSocket chat backend с horizontal scaling через Redis Pub/Sub, MongoDB history replay, token-bucket rate limiting, bounded backpressure queues, Prometheus metrics, Grafana dashboard и k6 load tests. | Kotlin, Java 17, Spring Boot, WebFlux, Reactor, Redis, MongoDB, Nginx, Prometheus, Grafana, k6 |
| [saas-billing-system](saas-billing-system/README.ru.md) | Production-like event-driven SaaS billing backend с service-owned PostgreSQL databases, Kafka commands/events, Debezium transactional outbox CDC, inbox idempotency, saga orchestration, WireMock PSP callbacks и full-system e2e tests для successful и failed initial billing. | Kotlin, Java 17, Spring Boot, JPA/Hibernate, PostgreSQL, Kafka, Kafka Connect, Debezium, Schema Registry, Avro, WireMock |

## Инженерный фокус

### Reactive Chat Service

`reactive-chat-service` демонстрирует stateful WebSocket backend, который может работать за Nginx в нескольких application instances. Он сохраняет critical messages в MongoDB, распределяет events между instances через Redis Pub/Sub, защищается token-bucket rate limiting и bounded outbound queues, а также экспортирует operational metrics для Prometheus/Grafana.

Load testing coverage включает:

- smoke WebSocket scenario;
- burst scenario с rate limiting;
- moderate multi-chat scenario;
- high-concurrency multi-chat scenario с `500` concurrent WebSocket sessions.

### SaaS Billing System

`saas-billing-system` демонстрирует distributed consistency patterns в production-like billing flow:

- database-per-service boundaries;
- transactional outbox with Debezium CDC into Kafka;
- inbox pattern для идемпотентных Kafka consumers;
- saga orchestration для initial billing;
- идемпотентное public subscription creation;
- deterministic PSP outcomes через WireMock;
- full-system e2e tests, которые проверяют service-owned database state.

Реализованные e2e scenarios покрывают оба terminal outcomes initial billing:

- successful payment: invoice `PAID`, subscription `ACTIVE`, payment attempt `SUCCEEDED`, saga `COMPLETED`;
- failed payment: invoice `PAYMENT_PENDING`, subscription `SUSPENDED`, payment attempt `FAILED`, saga `COMPLETED`.
