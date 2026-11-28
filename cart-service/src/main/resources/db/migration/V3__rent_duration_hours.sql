-- Replace rental_end date with rent_duration_hours (aligned with order planned duration).

ALTER TABLE carts DROP CONSTRAINT IF EXISTS carts_rental_period_chk;

ALTER TABLE carts DROP COLUMN IF EXISTS rental_end;

ALTER TABLE carts
    ADD COLUMN IF NOT EXISTS rent_duration_hours integer;

ALTER TABLE carts
    ADD CONSTRAINT carts_rental_duration_chk CHECK (
        rent_duration_hours IS NULL OR rent_duration_hours > 0
    );

COMMENT ON COLUMN carts.user_id IS 'Идентификатор пользователя (владелец корзины, из JWT)';
COMMENT ON COLUMN carts.rental_start IS 'Плановая дата начала аренды';
COMMENT ON COLUMN carts.rent_duration_hours IS 'Плановая длительность аренды, ч';
COMMENT ON COLUMN carts.pickup_point_id IS 'ID пункта выдачи';
COMMENT ON COLUMN carts.created_at IS 'Время создания записи';
COMMENT ON COLUMN carts.updated_at IS 'Время обновления записи';
