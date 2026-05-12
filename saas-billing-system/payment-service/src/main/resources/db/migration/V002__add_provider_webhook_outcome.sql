alter table payment_attempts
    add column completed_at timestamptz,
    add column failure_code varchar(255),
    add column failure_message varchar(1024);

create index idx_payment_attempts_provider_payment_id
    on payment_attempts(provider_payment_id);

create table provider_webhook_events (
    id uuid primary key,
    provider_event_id varchar(255) not null,
    provider_payment_id varchar(255) not null,
    type varchar(255) not null,
    status varchar(64) not null,
    received_at timestamptz not null,
    constraint uk_provider_webhook_events_provider_event_id
        unique (provider_event_id)
);

create index idx_provider_webhook_events_provider_payment_id
    on provider_webhook_events(provider_payment_id);
