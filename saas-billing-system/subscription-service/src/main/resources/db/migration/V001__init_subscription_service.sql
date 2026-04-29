create table subscriptions (
    id uuid primary key,
    organization_id varchar(255) not null,
    created_at timestamptz not null,
    status varchar(64) not null,
    subscription_plan varchar(64) not null,
    billing_period varchar(64) not null,
    seats integer not null,
    payment_method_token varchar(255) not null
);

create table subscription_changes (
    id uuid primary key,
    subscription_id uuid not null,
    requested_at timestamptz not null,
    new_plan varchar(64) not null,
    new_seats integer not null
);

create index idx_subscription_changes_subscription_id
    on subscription_changes(subscription_id);

create table subscription_history (
    id uuid primary key,
    subscription_id uuid not null,
    action varchar(128) not null,
    occurred_at timestamptz not null,
    details jsonb not null default '{}'::jsonb
);

create index idx_subscription_history_subscription_id
    on subscription_history(subscription_id);

create table idempotency_keys (
    id uuid primary key,
    organization_id varchar(255) not null,
    operation varchar(128) not null,
    idempotency_key varchar(255) not null,
    subscription_id uuid not null,
    created_at timestamptz not null,
    constraint uk_idempotency_keys_scope
        unique (organization_id, operation, idempotency_key)
);

create table outbox_messages (
    id uuid primary key,
    aggregatetype varchar(255) not null,
    aggregateid varchar(255) not null,
    type varchar(255) not null,
    payload jsonb not null,
    headers jsonb not null,
    timestamp timestamptz not null
);

create index idx_outbox_messages_aggregate
    on outbox_messages(aggregatetype, aggregateid);

create table inbox_messages (
    id uuid primary key,
    consumer varchar(255) not null,
    message_id uuid not null,
    message_type varchar(255) not null,
    aggregate_id varchar(255) not null,
    correlation_id uuid,
    causation_id uuid,
    received_at timestamptz not null,
    payload jsonb not null,
    headers jsonb not null,
    constraint uk_inbox_messages_consumer_message_id
        unique (consumer, message_id)
);

create index idx_inbox_messages_message_type
    on inbox_messages(message_type);
