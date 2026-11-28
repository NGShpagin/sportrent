-- Align orders / order_items with the current entity model (renames, money fields,
-- composite PK on order_items, jsonb characteristics).

ALTER TABLE orders RENAME COLUMN office_id TO pickup_point_id;
ALTER TABLE orders RENAME COLUMN planned_date TO planned_rent_start_date;
ALTER TABLE orders RENAME COLUMN rental_duration_hours TO planned_rental_duration_hours;

ALTER TABLE orders
    ADD COLUMN fixed_total_price  numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount    numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN used_points        integer        NOT NULL DEFAULT 0,
    ADD COLUMN amount_to_pay      numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN added_price        numeric(12, 2),
    ADD COLUMN accrued_points     integer;

ALTER TABLE orders
    ADD CONSTRAINT orders_money_chk CHECK (
        fixed_total_price >= 0
        AND discount_amount >= 0
        AND amount_to_pay >= 0
        AND (added_price IS NULL OR added_price >= 0)
        AND used_points >= 0
        AND (accrued_points IS NULL OR accrued_points >= 0)
        AND discount_amount <= fixed_total_price
    );

ALTER TABLE orders
    ADD CONSTRAINT orders_rental_duration_chk CHECK (
        planned_rental_duration_hours IS NULL OR planned_rental_duration_hours > 0
    );

COMMENT ON COLUMN orders.user_id IS 'Ссылка на айди пользователя';
COMMENT ON COLUMN orders.pickup_point_id IS 'Ссылка на ID пункта выдачи';
COMMENT ON COLUMN orders.planned_rent_start_date IS 'Плановая дата прихода в пункт выдачи';
COMMENT ON COLUMN orders.planned_rental_duration_hours IS 'Продолжительность аренды в часах';
COMMENT ON COLUMN orders.fixed_total_price IS 'Фиксированная полная сумма всех позиций';
COMMENT ON COLUMN orders.discount_amount IS 'Скидка по промокоду';
COMMENT ON COLUMN orders.used_points IS 'Количество списанных баллов';
COMMENT ON COLUMN orders.amount_to_pay IS 'Сумма к оплате при выдаче заказа';
COMMENT ON COLUMN orders.added_price IS 'Сумма доплаты после сдачи оборудования';
COMMENT ON COLUMN orders.accrued_points IS 'Начисленные баллы после завершения заказа';

ALTER TABLE order_items RENAME COLUMN catalog_item_id TO catalog_model_id;

ALTER TABLE order_items
    ADD COLUMN planned_quantity integer,
    ADD COLUMN item_include     jsonb;

UPDATE order_items
SET planned_quantity = GREATEST(COALESCE(issued_quantity, 1), 1)
WHERE planned_quantity IS NULL;

ALTER TABLE order_items
    ALTER COLUMN planned_quantity SET NOT NULL;

ALTER TABLE order_items DROP CONSTRAINT order_items_pkey;
ALTER TABLE order_items DROP COLUMN id;
ALTER TABLE order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (order_id, catalog_model_id);

ALTER TABLE order_items DROP CONSTRAINT order_items_quantity_chk;
ALTER TABLE order_items
    ADD CONSTRAINT order_items_quantity_chk CHECK (
        planned_quantity > 0
        AND (issued_quantity IS NULL OR issued_quantity >= 0)
        AND (returned_quantity IS NULL OR returned_quantity >= 0)
        AND (issued_quantity IS NULL OR issued_quantity <= planned_quantity)
        AND (issued_quantity IS NULL OR returned_quantity IS NULL
             OR returned_quantity <= issued_quantity)
    );

COMMENT ON COLUMN order_items.catalog_model_id IS 'Ссылка на ID товара в каталоге';
COMMENT ON COLUMN order_items.planned_quantity IS 'Забронированное количество';
COMMENT ON COLUMN order_items.item_include IS 'Характеристики забронированного товара';
