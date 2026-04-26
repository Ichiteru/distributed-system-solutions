# Mini SaaS Billing System: бизнес-требования

## 1. Контекст и цель

Нам нужна Billing System для B2B SaaS-продукта, где организации покупают подписку, выбирают тарифный план, указывают количество seats и оплачивают регулярные счета. Система должна поддерживать базовый жизненный цикл подписки и платежей, показывать понятные статусы для клиента и операционной команды, а также быть пригодной для дальнейшего расширения до production-like billing платформы.

В v1 система должна быть достаточно полной, чтобы демонстрировать реальные billing-сценарии: создание подписки, initial payment, renewal, изменение тарифа или seats со следующего периода, отмена подписки в конце периода и обработка неуспешных платежей через retry/grace/suspension.

## 2. Бизнес-цели

- Позволить организации оформить платную подписку на SaaS-продукт.
- Автоматически выставлять счета за подписку и количество seats.
- Автоматически активировать подписку после успешной оплаты initial invoice.
- Поддержать регулярное продление подписки по billing period.
- Дать клиенту возможность менять тариф и количество seats без ручного вмешательства.
- Обрабатывать неуспешные платежи предсказуемо: retry, grace period, затем ограничение доступа.
- Сохранить прозрачную историю invoices, payment attempts и изменений подписки.

## 3. Участники

- **Organization admin**: представитель клиента, который выбирает план, количество seats, меняет подписку и отменяет ее.
- **Billing operator**: внутренний пользователь, который просматривает состояние invoices, payment attempts и проблемных подписок.
- **SaaS platform**: продуктовая система, которая проверяет статус подписки перед предоставлением доступа.
- **Payment provider**: внешний платежный провайдер, который принимает платежи и присылает результат обработки.

## 4. Тарифы и денежные правила

В v1 доступны тарифы:

| План | Назначение | Billing model |
| --- | --- | --- |
| Basic | Малые команды | base subscription price + price per seat |
| Pro | Растущие команды | base subscription price + price per seat |
| Enterprise | Крупные клиенты | base subscription price + price per seat |

Общие правила:

- Поддерживаются monthly и yearly billing periods.
- Все суммы хранятся в USD minor units, например cents.
- Налоги, VAT, coupons, discounts, refunds и multi-currency не входят в v1.
- Card data не хранится в системе; система хранит только ссылку на payment method token.
- При изменении плана или seats proration не выполняется: изменение применяется со следующего billing period.

## 5. Основные бизнес-процессы

### 5.1 Создание подписки

1. Organization admin выбирает план, billing period, количество seats и payment method.
2. Система создает подписку в статусе `pending`.
3. Система создает initial invoice.
4. Система инициирует payment attempt через payment provider.
5. Если платеж успешен, invoice становится `paid`, подписка становится `active`.
6. Если платеж неуспешен, invoice переходит в failure handling flow.

Acceptance criteria:

- Повторная отправка одной и той же команды с тем же idempotency key не создает вторую подписку.
- Подписка не становится `active` до успешной оплаты initial invoice.
- Для initial invoice есть понятная связь с subscription, organization и payment attempt.

### 5.2 Регулярное продление

1. В конце текущего billing period система определяет подписки, которым нужен renewal.
2. Система создает renewal invoice на следующий период.
3. Система инициирует payment attempt.
4. При успешной оплате invoice становится `paid`, подписка остается `active`, следующий billing period фиксируется.
5. При ошибке оплаты запускается retry и grace period.

Acceptance criteria:

- Renewal invoice создается один раз на конкретный subscription period.
- Повторная обработка одного и того же renewal-события не создает дубликаты invoices.
- Успешная оплата продлевает доступ на следующий период.

### 5.3 Изменение плана или seats

1. Organization admin запрашивает upgrade, downgrade или изменение seats.
2. Система сохраняет pending change для следующего billing period.
3. Текущий invoice и текущий доступ не пересчитываются.
4. На следующем renewal invoice применяется новый план или новое количество seats.

Acceptance criteria:

- Изменение не влияет на уже оплаченный текущий период.
- Последний pending change является источником истины для следующего периода.
- В истории подписки видно, кто и когда запросил изменение.

### 5.4 Отмена подписки

1. Organization admin запрашивает отмену.
2. Система помечает подписку как `cancel_at_period_end`.
3. До конца оплаченного периода подписка остается доступной.
4. В конце периода подписка становится `canceled`, renewal invoice не создается.

Acceptance criteria:

- Отмена не удаляет подписку и billing history.
- Повторная отмена уже отменяемой подписки не меняет результат.
- После перехода в `canceled` новый renewal не запускается.

### 5.5 Неуспешная оплата

1. Payment provider возвращает failed result или не присылает успешный результат в ожидаемый срок.
2. Система помечает payment attempt как failed.
3. Система выполняет ограниченное количество retry попыток.
4. Пока идет grace period, подписка может оставаться доступной, но помечается как проблемная.
5. Если retry исчерпаны, invoice остается неоплаченным, подписка становится `past_due`, затем `suspended`.

Acceptance criteria:

- Для каждого failed payment attempt сохраняется причина ошибки.
- Retry не создает двойных списаний по одному invoice.
- После исчерпания retry подписка не остается в неопределенном состоянии.

## 6. Статусы

Subscription statuses:

- `pending`: подписка создана, initial invoice еще не оплачен.
- `active`: подписка оплачена и доступ разрешен.
- `past_due`: есть неоплаченный invoice, идет recovery flow.
- `suspended`: доступ ограничен из-за неоплаты.
- `cancel_at_period_end`: подписка активна до конца оплаченного периода, затем будет отменена.
- `canceled`: подписка завершена, renewal не выполняется.

Invoice statuses:

- `draft`: invoice создан, но еще не готов к оплате.
- `open`: invoice готов к оплате.
- `payment_pending`: платеж инициирован.
- `paid`: invoice оплачен.
- `failed`: оплата неуспешна, требуется retry или recovery action.
- `void`: invoice не должен быть оплачен.

Payment attempt statuses:

- `created`: попытка создана.
- `submitted`: запрос отправлен payment provider.
- `succeeded`: payment provider подтвердил оплату.
- `failed`: payment provider отклонил оплату.
- `timed_out`: результат не получен в ожидаемый срок.

## 7. Нефункциональные требования

- Система должна быть устойчива к повторной доставке команд и событий.
- Система не должна допускать двойную оплату одного invoice.
- Система должна сохранять audit trail для subscription, invoice и payment attempt changes.
- Система должна быть пригодна для локального запуска через Docker Compose.
- Система должна предоставлять диагностируемые статусы для зависших billing flows.
- Ошибки интеграции с payment provider не должны приводить к потере billing state.

## 8. Out of Scope для v1

- Налоги, VAT и tax provider integration.
- Multi-currency и currency conversion.
- Proration при изменении плана или seats.
- Refunds и chargebacks.
- Coupons, discounts и промокоды.
- Реальная интеграция со Stripe или другим PSP.
- Отдельный auth-service.
- Хранение card data или PCI-sensitive данных.
- Customer portal UI.
- Revenue recognition и бухгалтерская отчетность.

## 9. Definition of Done для бизнес-требований

- Можно создать подписку и довести ее до `active` через successful initial payment.
- Можно выполнить renewal и продлить subscription period.
- Можно запланировать изменение плана или seats на следующий период.
- Можно отменить подписку at period end.
- Можно смоделировать failed payment, retry, grace period и suspension.
- Для каждого критичного действия есть идемпотентное поведение.
- Для оператора доступна история invoices и payment attempts.
