-- Adds created_at / updated_at + trigger identical to the one on `orders`

-- Add columns if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'coupons'::regclass
          AND attname = 'created_at'
    ) THEN
        ALTER TABLE coupons
            ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'coupons'::regclass
          AND attname = 'updated_at'
    ) THEN
        ALTER TABLE coupons
            ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;
END$$;

--  Re-use (or create) the generic timestamp trigger
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to coupons
DROP TRIGGER IF EXISTS trg_coupons_set_updated_at ON coupons;
CREATE TRIGGER trg_coupons_set_updated_at
BEFORE UPDATE ON coupons
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

