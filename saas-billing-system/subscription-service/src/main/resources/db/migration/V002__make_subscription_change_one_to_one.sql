alter table subscription_changes
    add constraint uk_subscription_changes_subscription_id unique (subscription_id);
