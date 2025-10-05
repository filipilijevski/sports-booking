BEGIN;

    /* handy index for joins & look-ups */
    CREATE INDEX IF NOT EXISTS idx_refund_events_order
        ON refund_events(order_id);

    /* finance_ledger needs a fast refresh - this keeps REFRESH MATERIALIZED VIEW
       in single-digit ms for <50 k rows - should be good for our scale */
    CREATE UNIQUE INDEX IF NOT EXISTS idx_finance_ledger_id_ts
        ON finance_ledger(id, ts DESC);

COMMIT;

