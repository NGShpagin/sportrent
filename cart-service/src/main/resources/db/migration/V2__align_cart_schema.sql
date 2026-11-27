-- Align schema previously created by Hibernate ddl-auto with the current model.
-- Safe on fresh DBs where V1 already created the target shape.

ALTER TABLE carts ADD COLUMN IF NOT EXISTS rental_start date;
ALTER TABLE carts ADD COLUMN IF NOT EXISTS rental_end date;
ALTER TABLE carts ADD COLUMN IF NOT EXISTS pickup_point_id uuid;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'cart_items'
          AND column_name = 'item_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'cart_items'
          AND column_name = 'equipment_id'
    ) THEN
        ALTER TABLE cart_items RENAME COLUMN item_id TO equipment_id;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'carts_rental_period_chk'
    ) THEN
        ALTER TABLE carts
            ADD CONSTRAINT carts_rental_period_chk
            CHECK (rental_start IS NULL OR rental_end IS NULL OR rental_start <= rental_end);
    END IF;
END $$;
