alter table outbox_messages
    alter column "timestamp" type timestamp(3) without time zone
    using to_timestamp("timestamp" / 1000.0) at time zone 'UTC';
