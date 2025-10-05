-- provider_txn_id is globally unique anyway – enforce it
ALTER TABLE payment_events
    ADD CONSTRAINT uk_payment_events_txn UNIQUE (provider_txn_id);
