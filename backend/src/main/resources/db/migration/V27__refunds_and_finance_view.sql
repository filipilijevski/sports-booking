/*
  * Creates table refund_events (one-to-many order -> refund rows).
  * Re-creates finance_ledger as a MATERIALIZED VIEW combining:
      - purchases  (+)
      - refunds    (-)
  * All money values are stored in CAD dollars (numeric(10,2)).
*/

BEGIN;

    /*  refunds table  */
    CREATE TABLE IF NOT EXISTS refund_events (
        id               BIGSERIAL       PRIMARY KEY,
        order_id         BIGINT          NOT NULL REFERENCES orders(id),
        provider         VARCHAR(64)     NOT NULL,
        provider_txn_id  VARCHAR(128),
        amount           NUMERIC(10,2)   NOT NULL,     -- positive number
        currency         VARCHAR(8)      NOT NULL DEFAULT 'cad',
        status           VARCHAR(32),
        reason           VARCHAR(64),
        payload_json     JSONB,
        created_at       TIMESTAMPTZ     NOT NULL DEFAULT now()
    );

    /*  drop old materialised view (if any)  */
    DROP MATERIALIZED VIEW IF EXISTS finance_ledger;

    /*  build new unified finance view  */
    CREATE MATERIALIZED VIEW finance_ledger AS
    WITH
    purchases AS (
        SELECT
            o.id                            AS id,
            o.created_at                    AS ts,
            'PURCHASE'::text               AS action,
            o.subtotal_amount               AS subtotal,
            o.shipping_fee                  AS shipping,
            o.tax_amount                    AS tax,
            o.total_amount                  AS total,       -- positive
            o.status::text                  AS status       -- cast enum to text
        FROM orders o
    ),
    refunds AS (
        SELECT
            r.id                            AS id,
            r.created_at                    AS ts,
            'REFUND'::text                  AS action,
            -r.amount                       AS subtotal,    -- negated for netting
            0::numeric                      AS shipping,
            0::numeric                      AS tax,
            -r.amount                       AS total,       -- negative
            'REFUNDED'::text                AS status       -- typed literal
        FROM refund_events r
    )
    SELECT * FROM purchases
    UNION ALL
    SELECT * FROM refunds
    ORDER BY ts DESC;

    /*  index for fast CSV/date-range exports  */
    CREATE INDEX IF NOT EXISTS idx_finance_ledger_ts ON finance_ledger(ts);

COMMIT;

