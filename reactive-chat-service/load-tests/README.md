# k6 Load Tests

This directory contains k6 scenarios for `reactive-chat-service` burst and backpressure checks.

## Prerequisites

- Installed k6 CLI
- Running chat service endpoint

## Quick start

```bash
k6 run reactive-chat-service/load-tests/scenarios/ws-burst.js
```

You can override defaults with environment variables:

- `BASE_URL` (default: `ws://localhost:8080`)
- `CHAT_ID` (default: `chat-1`)
- `SENDER_ID` (default: `client-1`)

## Planned scenarios

- `ws-burst.js`: high-frequency message burst for overflow/reject behavior
- `ws-slow-consumer.js`: slow consumer simulation
- `ws-reconnect.js`: reconnect stress simulation
