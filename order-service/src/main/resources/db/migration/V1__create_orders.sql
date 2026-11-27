CREATE TABLE orders (
    id                      uuid         NOT NULL,
    user_id                 uuid         NOT NULL,
    customer_name           varchar(100) NOT NULL,
    customer_phone          varchar(20)  NOT NULL,
    office_id               uuid         NOT NULL,
    promocode_id            uuid,
    planned_date            date,
    rental_duration_hours   integer,
    actual_issued_at        timestamptz,
    actual_end_at           timestamptz,
    issued_by_employee_id   uuid,
    returned_by_employee_id uuid,
    order_status            varchar(20)  NOT NULL,
    created_at              timestamptz  NOT NULL,
    updated_at              timestamptz,
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT orders_actual_period_chk
        CHECK (actual_issued_at IS NULL OR actual_end_at IS NULL OR actual_issued_at <= actual_end_at)
);

COMMENT ON COLUMN orders.customer_name IS 'Имя клиента, на которого оформлен заказ';
COMMENT ON COLUMN orders.customer_phone IS 'Телефон клиента, на которого оформлен заказ';
COMMENT ON COLUMN orders.office_id IS 'Ссылка на ID пункта выдачи';
COMMENT ON COLUMN orders.promocode_id IS 'Ссылка на ID промокода';
COMMENT ON COLUMN orders.planned_date IS 'Плановая дата прихода в пункт выдачи';
COMMENT ON COLUMN orders.rental_duration_hours IS 'Продолжительность аренды в часах';
COMMENT ON COLUMN orders.actual_issued_at IS 'Фактическое время выдачи оборудования';
COMMENT ON COLUMN orders.actual_end_at IS 'Фактическое время возврата оборудования';
COMMENT ON COLUMN orders.issued_by_employee_id IS 'Ссылка на ID сотрудника пункта выдачи, выдавшего заказ';
COMMENT ON COLUMN orders.returned_by_employee_id IS 'Ссылка на ID сотрудника пункта выдачи, принявшего возврат';
COMMENT ON COLUMN orders.order_status IS 'Статус заказа';

CREATE TABLE order_items (
    id                uuid NOT NULL,
    order_id          uuid NOT NULL,
    catalog_item_id   uuid NOT NULL,
    issued_quantity   integer,
    returned_quantity integer,
    CONSTRAINT order_items_pkey PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT order_items_quantity_chk CHECK (
        (issued_quantity IS NULL OR issued_quantity >= 0)
        AND (returned_quantity IS NULL OR returned_quantity >= 0)
        AND (issued_quantity IS NULL OR returned_quantity IS NULL
             OR returned_quantity <= issued_quantity)
    )
);

COMMENT ON COLUMN order_items.order_id IS 'Ссылка на заказ';
COMMENT ON COLUMN order_items.catalog_item_id IS 'Ссылка на ID товара в каталоге';
COMMENT ON COLUMN order_items.issued_quantity IS 'Фактически выданное количество товара';
COMMENT ON COLUMN order_items.returned_quantity IS 'Фактически принятое количество товара';
