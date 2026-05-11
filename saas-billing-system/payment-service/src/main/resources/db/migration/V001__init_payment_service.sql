create table payment_methods (
    id uuid primary key,
    organization_id varchar(255) not null,
    token varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_payment_methods_organization_token
        unique (organization_id, token)
);

create index idx_payment_methods_organization_id
    on payment_methods(organization_id);

create table payment_attempts (
    id uuid primary key,
    invoice_id uuid not null,
    subscription_id uuid not null,
    organization_id varchar(255) not null,
    attempt_number integer not null,
    amount_minor bigint not null,
    currency varchar(3) not null,
    payment_method_token varchar(255) not null,
    status varchar(64) not null,
    provider_payment_id varchar(255),
    provider_status varchar(64),
    created_at timestamptz not null,
    submitted_at timestamptz,
    constraint uk_payment_attempts_invoice_attempt
        unique (invoice_id, attempt_number)
);

create index idx_payment_attempts_invoice_id
    on payment_attempts(invoice_id);

create index idx_payment_attempts_status
    on payment_attempts(status);

create table outbox_messages (
    id uuid primary key,
    aggregatetype varchar(255) not null,
    aggregateid varchar(255) not null,
    type varchar(255) not null,
    payload jsonb not null,
    headers jsonb not null,
    timestamp timestamp(3) without time zone not null
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
