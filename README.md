# Distributed System Solutions

Portfolio repository with practical backend projects focused on distributed systems, event-driven architecture, reactive programming, reliability patterns, observability, and load testing.

## Projects

| Project | Description | Stack |
| --- | --- | --- |
| [reactive-chat-service](reactive-chat-service/README.md) | Reactive low-latency WebSocket chat backend with horizontal scaling through Redis Pub/Sub, MongoDB history replay, token-bucket rate limiting, bounded backpressure queues, Prometheus metrics, Grafana dashboard, and k6 load tests. | Kotlin, Java 17, Spring Boot, WebFlux, Reactor, Redis, MongoDB, Nginx, Prometheus, Grafana, k6 |
| [saas-billing-system](saas-billing-system/README.md) | Production-like event-driven SaaS billing backend with service-owned PostgreSQL databases, Kafka commands/events, Debezium transactional outbox CDC, inbox idempotency, saga orchestration, WireMock PSP callbacks, and full-system e2e tests for successful and failed initial billing. | Kotlin, Java 17, Spring Boot, JPA/Hibernate, PostgreSQL, Kafka, Kafka Connect, Debezium, Schema Registry, Avro, WireMock |

## Engineering Focus

### Reactive Chat Service

`reactive-chat-service` demonstrates a stateful WebSocket backend that can run behind Nginx in multiple application instances. It stores critical messages in MongoDB, distributes events between instances through Redis Pub/Sub, protects itself with token-bucket rate limiting and bounded outbound queues, and exposes operational metrics for Prometheus/Grafana.

Load testing coverage includes:

- smoke WebSocket scenario;
- burst scenario with rate limiting;
- moderate multi-chat scenario;
- high-concurrency multi-chat scenario with `500` concurrent WebSocket sessions.

### SaaS Billing System

`saas-billing-system` demonstrates distributed consistency patterns in a production-like billing flow:

- database-per-service boundaries;
- transactional outbox with Debezium CDC into Kafka;
- inbox pattern for idempotent Kafka consumers;
- saga orchestration for initial billing;
- idempotent public subscription creation;
- deterministic PSP outcomes through WireMock;
- full-system e2e tests that verify service-owned database state.

The implemented e2e scenarios cover both terminal outcomes of initial billing:

- successful payment: invoice `PAID`, subscription `ACTIVE`, payment attempt `SUCCEEDED`, saga `COMPLETED`;
- failed payment: invoice `PAYMENT_PENDING`, subscription `SUSPENDED`, payment attempt `FAILED`, saga `COMPLETED`.
