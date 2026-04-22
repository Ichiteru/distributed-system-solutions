# Distributed System Solutions

Portfolio repository with practical backend projects focused on distributed systems, reactive programming, observability, and load testing.

## Projects

| Project | Description | Stack |
| --- | --- | --- |
| [reactive-chat-service](reactive-chat-service/README.md) | Reactive WebSocket chat backend with Redis Pub/Sub fan-out, MongoDB history replay, rate limiting, backpressure, Prometheus metrics, Grafana dashboard, and k6 load tests. | Kotlin, Java 17, Spring Boot, Spring WebFlux, Reactor, Redis, MongoDB, Nginx, Prometheus, Grafana, k6 |

## Current Focus

`reactive-chat-service` demonstrates a low-latency stateful WebSocket service that can run behind Nginx in two application instances. The service stores critical messages in MongoDB, distributes events between instances through Redis Pub/Sub, protects itself with token-bucket rate limiting and bounded outbound queues, and exposes operational metrics for Prometheus/Grafana.

Load testing results are documented in the project README, including:

- smoke WebSocket scenario;
- burst scenario with rate limiting;
- moderate multi-chat scenario;
- high-concurrency multi-chat scenario with `500` concurrent WebSocket sessions.
