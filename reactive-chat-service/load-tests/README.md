# k6 Load Tests

This directory contains k6 scenarios for `reactive-chat-service` WebSocket, backpressure, reconnect, and multi-instance checks.

## Prerequisites

- Installed k6 CLI
- Running chat service endpoint

## Quick start

```bash
k6 run reactive-chat-service/load-tests/scenarios/ws-smoke.js
```

You can override defaults with environment variables:

- `BASE_URL` (default: `ws://localhost:8080`)
- `CHAT_ID` (default: `chat-smoke`)
- `SENDER_ID` (default: `client-smoke-1`)
- `RECEIVER_ID` (default: `operator-smoke-1`)

Stop a running scenario with `Ctrl+C`. If the Docker stack should also be stopped, run:

```bash
cd reactive-chat-service
docker compose down
```

## Scenarios

### `ws-smoke.js`

Small functional scenario for validating that the deployed stack is usable before running heavier tests.

It opens two WebSocket connections in the same chat: one sender and one receiver. The sender emits typing events and two text messages. The scenario checks that both WebSocket handshakes succeed, the sender receives `chat.message.accepted`, and the receiver receives `chat.message.created`.

Run:

```bash
k6 run reactive-chat-service/load-tests/scenarios/ws-smoke.js
```

With custom endpoint:

```bash
BASE_URL=ws://localhost:8080 CHAT_ID=chat-smoke k6 run reactive-chat-service/load-tests/scenarios/ws-smoke.js
```

### `ws-burst.js`

High-frequency message burst for checking rate limiter, rejects, and service behavior under sudden load.

### `ws-pair-delivery.js`

Sender/receiver pair scenario for validating delivery path and status events such as `accepted`, `delivered`, and `rejected`.

### `ws-slow-consumer.js`

Slow receiver scenario for filling outbound queues and validating backpressure behavior.

### `ws-reconnect.js`

Disconnect/reconnect scenario for validating MongoDB history replay and ensuring ephemeral events are not replayed.

### `ws-multi-chat.js`

Multiple chats and users scenario for validating chat isolation and load distribution.

### `ws-long-lived.js`

Long-lived WebSocket scenario for checking memory, GC, and connection stability over time.
