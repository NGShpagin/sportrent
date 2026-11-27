CREATE TABLE carts (
    id              uuid        NOT NULL,
    user_id         uuid        NOT NULL,
    rental_start    date,
    rental_end      date,
    pickup_point_id uuid,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz,
    CONSTRAINT carts_pkey PRIMARY KEY (id),
    CONSTRAINT carts_rental_period_chk
        CHECK (rental_start IS NULL OR rental_end IS NULL OR rental_start <= rental_end)
);

CREATE TABLE cart_items (
    id           uuid        NOT NULL,
    cart_id      uuid        NOT NULL,
    equipment_id uuid        NOT NULL,
    quantity     integer     NOT NULL,
    added_at     timestamptz NOT NULL,
    CONSTRAINT cart_items_pkey PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id)
);
