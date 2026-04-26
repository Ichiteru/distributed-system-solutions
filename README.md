# Distributed System Solutions

Portfolio repository with practical backend projects focused on distributed systems, reactive programming, observability, and load testing.

## Projects

| Project | Description | Stack |
| --- | --- | --- |
| [reactive-chat-service](reactive-chat-service/README.md) | Reactive WebSocket chat backend with Redis Pub/Sub fan-out, MongoDB history replay, rate limiting, backpressure, Prometheus metrics, Grafana dashboard, and k6 load tests. | Kotlin, Java 17, Spring Boot, Spring WebFlux, Reactor, Redis, MongoDB, Nginx, Prometheus, Grafana, k6 |
| `saas-billing-system` | Event-driven B2B SaaS billing platform in progress with service-per-database boundaries, subscription lifecycle management, orchestrated billing flows, transactional outbox, and Debezium CDC-based event publication. Current implemented module: [subscription-service](saas-billing-system/subscription-service/README.md). | Kotlin, Java 17, Spring Boot, JPA/Hibernate, PostgreSQL, Kafka, Kafka Connect, Debezium, Schema Registry, Avro |

## Current Focus

`reactive-chat-service` demonstrates a low-latency stateful WebSocket service that can run behind Nginx in two application instances. The service stores critical messages in MongoDB, distributes events between instances through Redis Pub/Sub, protects itself with token-bucket rate limiting and bounded outbound queues, and exposes operational metrics for Prometheus/Grafana.

Load testing results are documented in the project README, including:

- smoke WebSocket scenario;
- burst scenario with rate limiting;
- moderate multi-chat scenario;
- high-concurrency multi-chat scenario with `500` concurrent WebSocket sessions.

`saas-billing-system` is a production-like billing architecture exercise centered on distributed consistency patterns: service-owned PostgreSQL databases, orchestrated billing flows, idempotent command handling, and Debezium Outbox CDC into Kafka. The repository currently contains the `subscription-service` module plus project-level architecture and business requirement documents.

