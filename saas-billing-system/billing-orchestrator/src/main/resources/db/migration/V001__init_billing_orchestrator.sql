create table billing_sagas (
    id uuid primary key,
    saga_type varchar(255) not null,
    business_key varchar(255) not null,
    status varchar(255) not null,
    correlation_id uuid,
    metadata jsonb not null default '{}'::jsonb,
    started_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    constraint uk_billing_sagas_type_business_key
        unique (saga_type, business_key)
);

create index idx_billing_sagas_status
    on billing_sagas(status);

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
