create table invoices (
    id uuid primary key,
    subscription_id uuid not null,
    organization_id varchar(255) not null,
    invoice_type varchar(64) not null,
    status varchar(64) not null,
    subscription_plan varchar(64) not null,
    billing_period varchar(64) not null,
    seats integer not null,
    period_start timestamptz not null,
    period_end timestamptz not null,
    amount_minor bigint not null,
    currency varchar(3) not null,
    payment_method_token varchar(255) not null,
    created_at timestamptz not null,
    constraint uk_invoices_subscription_period
        unique (subscription_id, invoice_type, period_start, period_end)
);

create index idx_invoices_subscription_id
    on invoices(subscription_id);

create index idx_invoices_status
    on invoices(status);

create table invoice_lines (
    id uuid primary key,
    invoice_id uuid not null references invoices(id) on delete cascade,
    description varchar(255) not null,
    quantity integer not null,
    amount_minor bigint not null,
    currency varchar(3) not null
);

create index idx_invoice_lines_invoice_id
    on invoice_lines(invoice_id);

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
