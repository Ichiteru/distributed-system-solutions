create table command_outbox (
    id uuid primary key,
    destination_topic varchar(255) not null,
    aggregatetype varchar(255) not null,
    aggregateid varchar(255) not null,
    type varchar(255) not null,
    payload jsonb not null,
    headers jsonb not null,
    timestamp timestamp(3) without time zone not null
);

create index idx_command_outbox_destination_topic_timestamp
    on command_outbox(destination_topic, timestamp);

create index idx_command_outbox_aggregate
    on command_outbox(aggregatetype, aggregateid);
