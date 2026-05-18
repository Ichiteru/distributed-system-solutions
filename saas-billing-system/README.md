# SaaS Billing System

Production-like event-driven billing system used to model subscription, invoice, payment and saga flows with local PostgreSQL databases, Kafka, Debezium outbox/inbox messaging and WireMock PSP callbacks.

## End-to-End Integration Tests

Keep the full-system e2e suite small and scenario-focused. These tests should run the local application flow across service boundaries and assert final state in service-owned databases.

- Successful initial billing:
  `POST /subscriptions` with a normal payment token creates a pending subscription, creates an invoice, submits payment, receives `PaymentSucceeded`, marks the invoice `PAID`, activates the subscription and completes the saga.

- Failed initial billing:
  `POST /subscriptions` with `paymentMethodToken = "pm_fail"` creates a pending subscription, creates an invoice, submits payment, receives `PaymentFailed`, marks the invoice `PAYMENT_PENDING`, suspends the subscription and completes the saga.
