CREATE TABLE promocodes (
    id         uuid         NOT NULL,
    name       varchar(24)  NOT NULL,
    start_date timestamptz  NOT NULL,
    end_date   timestamptz  NOT NULL,
    is_active  boolean      NOT NULL,
    type       varchar(255) NOT NULL,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz,
    deleted_at timestamptz,
    CONSTRAINT promocodes_pkey PRIMARY KEY (id),
    CONSTRAINT promocodes_period_chk CHECK (start_date <= end_date)
);

COMMENT ON COLUMN promocodes.name IS 'Название промокода';
COMMENT ON COLUMN promocodes.start_date IS 'Начало действия промокода';
COMMENT ON COLUMN promocodes.end_date IS 'Конец действия промокода';
COMMENT ON COLUMN promocodes.is_active IS 'Статус';
COMMENT ON COLUMN promocodes.type IS 'Тип промокода';

CREATE TABLE promocodes_used (
    id           uuid        NOT NULL,
    promocode_id uuid        NOT NULL,
    user_id      uuid        NOT NULL,
    created_at   timestamptz NOT NULL,
    CONSTRAINT promocodes_used_pkey PRIMARY KEY (id),
    CONSTRAINT fk_promocodes_used_promocode
        FOREIGN KEY (promocode_id) REFERENCES promocodes (id)
);

COMMENT ON COLUMN promocodes_used.promocode_id IS 'ID промокода';
COMMENT ON COLUMN promocodes_used.user_id IS 'ID пользователя';
