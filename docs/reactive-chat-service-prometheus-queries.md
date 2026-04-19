# Reactive Chat Service Prometheus Queries

## Sessions

```promql
chat_ws_sessions_active
```

## Outbound Backpressure

```promql
chat_outbound_buffer_size
```

```promql
rate(chat_outbound_events_dropped_total[5m])
```

## Message Rejects

```promql
rate(chat_messages_rejected_total[5m])
```

```promql
chat_messages_reject_rate
```

## Delivery Latency

```promql
histogram_quantile(0.95, rate(chat_delivery_latency_seconds_bucket[5m]))
```

```promql
histogram_quantile(0.99, rate(chat_delivery_latency_seconds_bucket[5m]))
```

## Redis Pub/Sub

```promql
rate(chat_redis_events_published_total[5m])
```

```promql
rate(chat_redis_events_consumed_total[5m])
```

## Rate Limiter

```promql
rate(chat_rate_limit_requests_total[5m])
```

```promql
sum by (outcome) (rate(chat_rate_limit_requests_total[5m]))
```
