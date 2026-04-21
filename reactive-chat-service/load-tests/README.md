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

The scenario opens many short-lived sender WebSocket connections. Each connection sends a burst of
`chat.message.created` events with the current event envelope contract and then waits for server responses.

Acceptance metrics:

- `ws_burst_messages_sent_total`: total messages sent by k6.
- `ws_burst_accepted_total`: `chat.message.accepted` responses received by senders.
- `ws_burst_rejected_total`: `chat.message.rejected` responses received by senders.
- `ws_burst_error_total`: `error` envelopes or unparsable server messages.
- `ws_burst_connection_errors_total`: WebSocket-level connection errors.
- `ws_burst_response_latency_ms`: time from client send to first server response with the same `correlationId`.

Configuration:

- `BASE_URL` (default: `ws://localhost:8080`)
- `CHAT_ID` (default: `chat-burst`)
- `USER_ID_PREFIX` (default: `client-burst`)
- `VUS` (default: `50`)
- `DURATION` (default: `30s`)
- `MESSAGES_PER_CONNECTION` (default: `100`)
- `SEND_INTERVAL_MS` (default: `0`, send the whole burst immediately)
- `SOCKET_LIFETIME_MS` (default: `10000`)

Light run:

```bash
VUS=5 DURATION=10s MESSAGES_PER_CONNECTION=10 k6 run reactive-chat-service/load-tests/scenarios/ws-burst.js
```

Rate-limiter run:

```bash
VUS=50 DURATION=30s MESSAGES_PER_CONNECTION=100 k6 run reactive-chat-service/load-tests/scenarios/ws-burst.js
```

Less aggressive run with a small delay between messages:

```bash
VUS=20 DURATION=30s MESSAGES_PER_CONNECTION=50 SEND_INTERVAL_MS=20 k6 run reactive-chat-service/load-tests/scenarios/ws-burst.js
```

Stop with `Ctrl+C`. For Docker demo stack cleanup:

```bash
cd reactive-chat-service
docker compose down
```

### `ws-pair-delivery.js`

Sender/receiver pair scenario for validating delivery path and status events such as `accepted`, `delivered`, and `rejected`.

### `ws-slow-consumer.js`

Slow receiver scenario for filling outbound queues and validating backpressure behavior.

### `ws-reconnect.js`

Disconnect/reconnect scenario for validating MongoDB history replay and ensuring ephemeral events are not replayed.

### `ws-multi-chat.js`

Multiple chats and users scenario for validating chat isolation, fan-out delivery, and load distribution.

The scenario starts `USERS_PER_CHAT` active WebSocket sessions per chat. Every session acts as a real chat
participant: it sends `chat.message.created` events and receives messages from the other active sessions in
the same `chatId`. For each sender in a chat with `N` active users, the expected fan-out is `N - 1`
receivers.

If a session receives `chat.message.created` from another `chatId`, the scenario increments
`ws_multi_chat_cross_chat_leaks_total` and fails the threshold. If a session receives its own message back,
the scenario increments `ws_multi_chat_self_delivery_leaks_total` and fails the threshold.

The service sends MongoDB history on every WebSocket reconnect. The scenario ignores replayed history when
checking self-delivery and cross-chat leaks by counting only `chat.message.created` events whose envelope
`timestamp` is not older than the current socket open time.

Acceptance metrics:

- `ws_multi_chat_messages_sent_total`: total messages sent by all senders.
- `ws_multi_chat_accepted_total`: `chat.message.accepted` responses received by message senders.
- `ws_multi_chat_received_total`: `chat.message.created` events received by non-sender sessions in the same chat.
- `ws_multi_chat_cross_chat_leaks_total`: messages delivered to the wrong `chatId`; must stay `0`.
- `ws_multi_chat_self_delivery_leaks_total`: messages delivered back to the sender; must stay `0`.
- `ws_multi_chat_error_events_total`: `error` envelopes or unparsable server messages.
- `ws_multi_chat_connection_errors_total`: WebSocket-level connection errors.
- `ws_multi_chat_response_latency_ms`: time from client send to first server response with the same `correlationId`.

Configuration:

- `BASE_URL` (default: `ws://localhost:8080`)
- `CHAT_ID_PREFIX` (default: `chat-multi`)
- `USER_ID_PREFIX` (default: `multi-user`)
- `CHAT_COUNT` (default: `5`)
- `USERS_PER_CHAT` (default: `5`)
- `DURATION` (default: `30s`)
- `MESSAGES_PER_CONNECTION` (default: `5`)
- `SEND_INTERVAL_MS` (default: `100`)
- `SEND_START_DELAY_MS` (default: `1000`, gives all users time to connect before sending)
- `SOCKET_LIFETIME_MS` (default: `35000`)

Light run:

```bash
CHAT_COUNT=3 USERS_PER_CHAT=3 DURATION=15s MESSAGES_PER_CONNECTION=3 k6 run reactive-chat-service/load-tests/scenarios/ws-multi-chat.js
```

Default multi-chat run:

```bash
k6 run reactive-chat-service/load-tests/scenarios/ws-multi-chat.js
```

Higher concurrency run:

```bash
CHAT_COUNT=10 USERS_PER_CHAT=5 DURATION=30s MESSAGES_PER_CONNECTION=5 SEND_INTERVAL_MS=100 k6 run reactive-chat-service/load-tests/scenarios/ws-multi-chat.js
```

### `ws-long-lived.js`

Long-lived WebSocket scenario for checking memory, GC, and connection stability over time.
