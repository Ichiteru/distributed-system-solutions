# Mini SaaS Billing System: внутренняя архитектура сервисов

## 1. Цель документа

Этот документ фиксирует внутреннюю архитектуру сервисов Mini SaaS Billing System. Он дополняет общий архитектурный документ и отвечает на вопрос: как организовывать код внутри каждого deployable сервиса, где держать доменную модель, JPA entities, transaction boundaries, REST/Kafka adapters, outbox/inbox и интеграции.

Целевой стиль: **modular hexagonal architecture + DDD-lite**. Это не попытка построить академически чистую архитектуру, а практичная структура для billing-системы с понятными границами и минимумом случайных зависимостей.

## 2. Базовый принцип

Каждый сервис делится на три основных слоя:

- `domain`: бизнес-модель и бизнес-правила сервиса.
- `application`: use cases, command handlers, transaction boundaries, orchestration внутри одного сервиса.
- `infrastructure`: Spring, REST, Kafka, JPA, PostgreSQL, WireMock client, configuration, metrics.

Главное правило зависимостей:

```text
infrastructure -> application -> domain
```

Обратные зависимости запрещены:

- `domain` не знает про Spring, JPA, Kafka, REST и JSON.
- `application` не знает про HTTP controllers, Kafka listener classes и JPA entity details.
- `infrastructure` адаптирует внешний мир к application ports/use cases.

## 3. Стандартный package layout сервиса

Рекомендуемый layout для каждого сервиса:

```text
<service>/
  src/main/kotlin/com/ilchern/saasbilling/<service>/
    <Service>Application.kt

    domain/
      model/
      event/
      repository/
      service/

    application/
      command/
      handler/
      port/
      service/

    infrastructure/
      config/
      clock/
      messaging/
        kafka/
        outbox/
        inbox/
      persistence/
        entity/
        mapper/
        repository/
      web/
        api/
        dto/
        mapper/
```

Если сервису нужен внешний HTTP-клиент, например `payment-service` для WireMock PSP, добавляется:

```text
infrastructure/
  provider/
    wiremock/
```

Если сервису нужны scheduled jobs, например outbox publisher или reconciliation, добавляется:

```text
infrastructure/
  scheduler/
```

## 4. Domain Layer

`domain` содержит только бизнесовые понятия сервиса:

- aggregates;
- entities;
- value objects;
- enums;
- domain events;
- domain services;
- repository interfaces, если они нужны доменному/application коду.

Примеры:

```text
domain/model/Subscription.kt
domain/model/SubscriptionStatus.kt
domain/model/BillingPeriod.kt
domain/event/SubscriptionCreated.kt
domain/repository/SubscriptionRepository.kt
domain/service/SubscriptionPolicy.kt
```

Правила:

- Не использовать `@Entity`, `@Table`, `@Column`, `@Transactional`, `@Service`, `@Component`.
- Не использовать Kafka/HTTP DTO в domain model.
- Не хранить business logic в JPA entity.
- Не делать domain model anemic, если поведение естественно принадлежит агрегату.

Пример допустимого доменного поведения:

```kotlin
class Subscription(
  val id: SubscriptionId,
  val organizationId: OrganizationId,
  private var status: SubscriptionStatus,
) {
  fun activate() {
    check(status == SubscriptionStatus.PENDING) { "Only pending subscription can be activated" }
    status = SubscriptionStatus.ACTIVE
  }
}
```

## 5. Application Layer

`application` содержит use cases и command handlers. Это основной слой для транзакционных бизнес-операций.

Типовой flow:

```text
REST/Kafka adapter
  -> application handler
    -> load aggregate
    -> execute domain behavior
    -> save aggregate
    -> save outbox message
```

Правила:

- `@Transactional` ставится на application handler/application service.
- Controller и Kafka listener не должны быть transaction boundary.
- Application layer работает с domain repositories и application ports.
- Application layer не должен работать напрямую с `EntityManager`, JPA entity или KafkaTemplate.

Пример:

```text
application/command/CreateSubscriptionCommand.kt
application/handler/CreateSubscriptionHandler.kt
application/port/OutboxMessageStore.kt
application/port/PaymentProviderClient.kt
```

## 6. Infrastructure Layer

`infrastructure` содержит техническую реализацию портов и adapters:

- REST controllers;
- request/response DTO;
- Kafka listeners/producers;
- JPA entities;
- Spring Data repositories;
- persistence mappers;
- outbox/inbox storage;
- scheduler jobs;
- provider clients;
- Spring configuration.

Правила:

- REST DTO не передаются в application/domain напрямую.
- Kafka message DTO не передаются в domain напрямую.
- JPA entity не используется как domain aggregate.
- Mappers живут на границе infrastructure.

## 7. JPA, Hibernate и EntityGraph

В v1 доменные модели и JPA entities разделяются.

Рекомендуемый вариант:

```text
domain/model/Invoice.kt
infrastructure/persistence/entity/InvoiceEntity.kt
infrastructure/persistence/mapper/InvoicePersistenceMapper.kt
infrastructure/persistence/repository/InvoiceJpaRepository.kt
```

Причины:

- domain model остается независимой от Hibernate lifecycle;
- бизнес-тесты не требуют Spring context;
- lazy loading не протекает в application/domain code;
- EntityGraph используется осознанно в persistence adapter;
- можно явно контролировать read use cases.

EntityGraph применять для query/use cases, где нужно загрузить агрегат со связанными сущностями без N+1:

```text
Invoice with lines
Subscription with pending changes
BillingSaga with steps
PaymentAttempt with provider events
```

Не использовать EntityGraph как способ случайно подтянуть все связи. Для каждого use case должен быть явный repository method.

## 8. Transaction Boundaries

Транзакция начинается в application layer.

Допустимо:

```text
application/handler/CreateInvoiceHandler.handle(...)
application/handler/HandlePaymentSucceededHandler.handle(...)
application/handler/StartInitialBillingSagaHandler.handle(...)
```

Не делать:

```text
web/api/*Controller
messaging/kafka/*Listener
domain/model/*
infrastructure/persistence/repository/*
```

Kafka consumer с inbox должен работать так:

```text
Kafka listener
  -> InboxMessageProcessor
    -> application handler @Transactional
      -> insert/check inbox message
      -> execute business change
      -> save outbox messages
      -> mark inbox message processed
```

Если сообщение уже обработано, handler должен завершиться без повторных side effects.

## 9. Outbox и Inbox

Outbox и inbox являются infrastructure patterns, но вызываются из application layer через порты.

Рекомендуемые application ports:

```text
application/port/OutboxMessageStore.kt
application/port/InboxMessageStore.kt
```

Реализация:

```text
infrastructure/messaging/outbox/JpaOutboxMessageStore.kt
infrastructure/messaging/outbox/OutboxPublisherJob.kt
infrastructure/messaging/inbox/JpaInboxMessageStore.kt
infrastructure/messaging/inbox/InboxMessageProcessor.kt
```

Правила:

- Бизнес-изменение и запись outbox message выполняются в одной PostgreSQL-транзакции.
- Inbox record, бизнес-изменение и outbox messages для следующего шага выполняются в одной PostgreSQL-транзакции.
- Outbox publisher не содержит бизнес-логики.
- Inbox processor не содержит бизнес-логики, только техническую дедупликацию и вызов handler.

## 10. Kafka Adapters

Kafka listener относится к infrastructure layer.

Задачи listener:

- принять Kafka record;
- распарсить message envelope;
- провалидировать базовые technical headers;
- передать команду/событие в application handler;
- не выполнять бизнес-логику.

Задачи publisher:

- читать outbox messages;
- сериализовать message envelope;
- публиковать в нужный topic;
- пометить outbox record как published.

Message contracts могут жить в отдельном Gradle module `billing-contracts`, но только как технический контракт. Не класть туда domain aggregates.

Допустимо в `billing-contracts`:

```text
MessageEnvelope
SubscriptionCreatedEvent
CreateInitialInvoiceCommand
PaymentSucceededEvent
```

Не допустимо:

```text
Subscription
Invoice
PaymentAttempt
BillingSaga
```

## 11. REST Adapters

REST controller относится к infrastructure layer.

Задачи controller:

- принять HTTP request;
- прочитать headers;
- выполнить request DTO validation;
- собрать application command;
- вызвать application handler;
- вернуть response DTO.

Controller не должен:

- открывать транзакцию;
- работать с JPA repository;
- публиковать Kafka messages напрямую;
- менять domain object напрямую.

## 12. Структура subscription-service

```text
subscription-service/
  domain/
    model/
      Subscription.kt
      SubscriptionStatus.kt
      SubscriptionPlan.kt
      BillingPeriod.kt
      PendingSubscriptionChange.kt
    event/
      SubscriptionCreated.kt
      SubscriptionChangeScheduled.kt
      SubscriptionCancellationRequested.kt
    repository/
      SubscriptionRepository.kt

  application/
    command/
      CreateSubscriptionCommand.kt
      ScheduleSubscriptionChangeCommand.kt
      CancelSubscriptionCommand.kt
      ActivateSubscriptionCommand.kt
      MarkSubscriptionPastDueCommand.kt
      SuspendSubscriptionCommand.kt
    handler/
      CreateSubscriptionHandler.kt
      ScheduleSubscriptionChangeHandler.kt
      CancelSubscriptionHandler.kt
      ApplySubscriptionOutcomeHandler.kt

  infrastructure/
    web/api/
      SubscriptionController.kt
    messaging/kafka/
      SubscriptionCommandListener.kt
    persistence/
      entity/
      mapper/
      repository/
```

Основной агрегат: `Subscription`.

Главные инварианты:

- initial subscription создается в `pending`;
- `active` возможен только после successful initial payment;
- cancel применяется at period end;
- plan/seats changes применяются со следующего billing period.

## 13. Структура billing-service

```text
billing-service/
  domain/
    model/
      Invoice.kt
      InvoiceLine.kt
      InvoiceStatus.kt
      Money.kt
      BillingPeriod.kt
    event/
      InvoiceCreated.kt
      InvoicePaid.kt
      InvoiceFailed.kt
    repository/
      InvoiceRepository.kt

  application/
    command/
      CreateInitialInvoiceCommand.kt
      CreateRenewalInvoiceCommand.kt
      MarkInvoicePaidCommand.kt
      MarkInvoicePaymentFailedCommand.kt
    handler/
      CreateInitialInvoiceHandler.kt
      CreateRenewalInvoiceHandler.kt
      MarkInvoicePaidHandler.kt
      MarkInvoicePaymentFailedHandler.kt
    service/
      InvoicePricingService.kt

  infrastructure/
    messaging/kafka/
      BillingCommandListener.kt
      PaymentEventListener.kt
    persistence/
      entity/
      mapper/
      repository/
```

Основной агрегат: `Invoice`.

Главные инварианты:

- один invoice на subscription period;
- `paid` invoice не может снова стать `payment_pending` или `failed`;
- invoice amount считается в minor units;
- payment success/failure применяется идемпотентно.

## 14. Структура payment-service

```text
payment-service/
  domain/
    model/
      PaymentAttempt.kt
      PaymentAttemptStatus.kt
      PaymentMethod.kt
      ProviderPaymentReference.kt
    event/
      PaymentSucceeded.kt
      PaymentFailed.kt
    repository/
      PaymentAttemptRepository.kt

  application/
    command/
      SubmitPaymentCommand.kt
      HandleProviderWebhookCommand.kt
    handler/
      SubmitPaymentHandler.kt
      HandleProviderWebhookHandler.kt
    port/
      PaymentProviderClient.kt

  infrastructure/
    provider/wiremock/
      WireMockPaymentProviderClient.kt
    web/api/
      PaymentProviderWebhookController.kt
    messaging/kafka/
      PaymentCommandListener.kt
    persistence/
      entity/
      mapper/
      repository/
```

Основной агрегат: `PaymentAttempt`.

Главные инварианты:

- один payment attempt имеет один финальный outcome;
- provider webhook дедуплицируется по provider event id;
- повторный `SubmitPaymentCommand` с тем же business key не создает двойное списание;
- card data не хранится.

## 15. Структура billing-orchestrator

```text
billing-orchestrator/
  domain/
    model/
      BillingSaga.kt
      SagaStep.kt
      SagaStatus.kt
      RetryPolicy.kt
    event/
      BillingSagaStarted.kt
      BillingSagaCompleted.kt
      BillingSagaFailed.kt
    repository/
      BillingSagaRepository.kt

  application/
    command/
      StartInitialBillingSagaCommand.kt
      HandleInvoiceCreatedCommand.kt
      HandleInvoicePaidCommand.kt
      HandlePaymentFailedCommand.kt
      RunSagaReconciliationCommand.kt
    handler/
      StartInitialBillingSagaHandler.kt
      HandleInvoiceCreatedHandler.kt
      HandleInvoicePaidHandler.kt
      HandlePaymentFailedHandler.kt
      RunSagaReconciliationHandler.kt
    service/
      SagaDecisionService.kt

  infrastructure/
    messaging/kafka/
      SagaEventListener.kt
      SagaCommandPublisher.kt
    scheduler/
      SagaReconciliationJob.kt
    persistence/
      entity/
      mapper/
      repository/
```

Основной агрегат: `BillingSaga`.

Главные инварианты:

- одна saga на одну business operation;
- orchestrator не владеет subscription/invoice/payment state;
- orchestrator хранит только process state;
- все исходящие команды публикуются через outbox.

## 16. Shared Modules

Рекомендуемые Gradle modules:

```text
saas-billing-system/
  billing-contracts/
  subscription-service/
  billing-service/
  payment-service/
  billing-orchestrator/
```

Опционально позже:

```text
billing-test-support/
```

Правила shared modules:

- `billing-contracts` содержит только Kafka contracts и общий message envelope.
- Не создавать shared domain module.
- Не выносить service-specific business logic в общий код.
- Общие test utilities выносить только после появления реального повторения в тестах.

## 17. Naming Rules

Для command handlers:

```text
<Action><Aggregate>Handler
CreateSubscriptionHandler
CreateInitialInvoiceHandler
SubmitPaymentHandler
HandlePaymentFailedHandler
```

Для application commands:

```text
<Action><Aggregate>Command
CreateSubscriptionCommand
SubmitPaymentCommand
```

Для Kafka listeners:

```text
<Domain>CommandListener
<Domain>EventListener
PaymentCommandListener
SagaEventListener
```

Для repositories:

```text
domain/repository/InvoiceRepository.kt
infrastructure/persistence/repository/JpaInvoiceRepositoryAdapter.kt
infrastructure/persistence/repository/SpringDataInvoiceJpaRepository.kt
```

## 18. Testing Layout

Рекомендуемый test layout внутри каждого сервиса:

```text
src/test/kotlin/.../
  domain/
  application/
  infrastructure/
```

Что тестировать:

- `domain`: быстрые unit tests без Spring.
- `application`: use case tests с mocked ports или lightweight fakes.
- `infrastructure/persistence`: JPA + PostgreSQL Testcontainers.
- `infrastructure/messaging`: Kafka serialization, outbox/inbox behavior.
- `infrastructure/web`: controller slice tests.

End-to-end сценарии лучше держать отдельно на уровне всего `saas-billing-system`, когда появится docker-compose/test orchestration.

## 19. Решения по умолчанию

- Использовать hexagonal architecture во всех сервисах одинаково.
- Разделять domain model и JPA entity.
- Держать `@Transactional` только в application layer.
- Делать outbox/inbox обязательными для write-flow.
- Не использовать shared domain model.
- Не использовать R2DBC/WebFlux в v1.
- Использовать EntityGraph только в persistence adapter для конкретных use cases.
- Дублировать небольшие value objects между сервисами допустимо, если это сохраняет независимость bounded contexts.
