alter table outbox_messages
    alter column "timestamp" type bigint
    using (extract(epoch from "timestamp") * 1000)::bigint;
