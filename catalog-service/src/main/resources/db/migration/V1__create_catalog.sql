CREATE TABLE categories (
    id         uuid         NOT NULL,
    name       varchar(255) NOT NULL,
    updated_at timestamptz  NOT NULL,
    CONSTRAINT categories_pkey PRIMARY KEY (id)
);

COMMENT ON COLUMN categories.name IS 'Название категории';

CREATE TABLE models (
    id             uuid           NOT NULL,
    name           varchar(255)   NOT NULL,
    brand          varchar(255)   NOT NULL,
    category_id    uuid           NOT NULL,
    price_per_hour numeric(12, 2) NOT NULL,
    is_available   boolean        NOT NULL,
    created_at     timestamptz    NOT NULL,
    updated_at     timestamptz    NOT NULL,
    deleted_at     timestamptz,
    CONSTRAINT models_pkey PRIMARY KEY (id),
    CONSTRAINT models_name_key UNIQUE (name),
    CONSTRAINT fk_models_category
        FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT models_price_chk CHECK (price_per_hour >= 0)
);

COMMENT ON COLUMN models.name IS 'Название модели';
COMMENT ON COLUMN models.brand IS 'Название бренда модели';
COMMENT ON COLUMN models.category_id IS 'ID категории';
COMMENT ON COLUMN models.price_per_hour IS 'Стоимость аренды модели за 1 час';
COMMENT ON COLUMN models.is_available IS 'Признак глобальной доступности модели';
COMMENT ON COLUMN models.created_at IS 'Время создания записи';
COMMENT ON COLUMN models.updated_at IS 'Время обновления записи';
COMMENT ON COLUMN models.deleted_at IS 'Время удаления записи';

CREATE TABLE pickup_points (
    id            uuid        NOT NULL,
    name          varchar(255),
    address       varchar(255),
    description   text,
    metro_station varchar(255),
    open_time     time,
    close_time    time,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    deleted_at    timestamptz,
    CONSTRAINT pickup_points_pkey PRIMARY KEY (id),
    CONSTRAINT pickup_points_hours_chk CHECK (
        open_time IS NULL OR close_time IS NULL OR open_time < close_time
    )
);

COMMENT ON COLUMN pickup_points.name IS 'Название пункта выдачи';
COMMENT ON COLUMN pickup_points.address IS 'Адрес пункта выдачи';
COMMENT ON COLUMN pickup_points.description IS 'Краткое описание';
COMMENT ON COLUMN pickup_points.metro_station IS 'Ближайшее метро к пункту выдачи';
COMMENT ON COLUMN pickup_points.open_time IS 'Время открытия пункта выдачи';
COMMENT ON COLUMN pickup_points.close_time IS 'Время закрытия пункта выдачи';
COMMENT ON COLUMN pickup_points.created_at IS 'Время создания записи';
COMMENT ON COLUMN pickup_points.updated_at IS 'Время обновления записи';
COMMENT ON COLUMN pickup_points.deleted_at IS 'Время удаления записи';

CREATE TABLE items (
    id            uuid         NOT NULL,
    name          varchar(255) NOT NULL,
    description   text,
    article       varchar(255) NOT NULL,
    serial_number varchar(255) NOT NULL,
    model_id      uuid         NOT NULL,
    status        varchar(32)  NOT NULL,
    specification jsonb,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    deleted_at    timestamptz,
    CONSTRAINT items_pkey PRIMARY KEY (id),
    CONSTRAINT items_serial_number_key UNIQUE (serial_number),
    CONSTRAINT fk_items_model
        FOREIGN KEY (model_id) REFERENCES models (id)
);

COMMENT ON COLUMN items.name IS 'Название товара';
COMMENT ON COLUMN items.description IS 'Описание';
COMMENT ON COLUMN items.article IS 'Артикул';
COMMENT ON COLUMN items.serial_number IS 'Серийный номер';
COMMENT ON COLUMN items.model_id IS 'Модель';
COMMENT ON COLUMN items.status IS 'Статус единицы товара';
COMMENT ON COLUMN items.specification IS 'Характеристики единицы товара';
COMMENT ON COLUMN items.created_at IS 'Время создания записи';
COMMENT ON COLUMN items.updated_at IS 'Время обновления записи';
COMMENT ON COLUMN items.deleted_at IS 'Время удаления записи';

CREATE TABLE model_files (
    id           uuid         NOT NULL,
    model_id     uuid         NOT NULL,
    link_file_id varchar(255) NOT NULL,
    CONSTRAINT model_files_pkey PRIMARY KEY (id),
    CONSTRAINT fk_model_files_model
        FOREIGN KEY (model_id) REFERENCES models (id)
);

COMMENT ON COLUMN model_files.model_id IS 'ID модели';
COMMENT ON COLUMN model_files.link_file_id IS 'url на хранилище s3';

CREATE TABLE model_pickup_points (
    model_id        uuid    NOT NULL,
    pickup_point_id uuid    NOT NULL,
    is_available    boolean NOT NULL,
    updated_at      timestamptz,
    CONSTRAINT model_pickup_points_pkey PRIMARY KEY (model_id, pickup_point_id),
    CONSTRAINT fk_model_pickup_points_model
        FOREIGN KEY (model_id) REFERENCES models (id),
    CONSTRAINT fk_model_pickup_points_pickup_point
        FOREIGN KEY (pickup_point_id) REFERENCES pickup_points (id)
);

COMMENT ON COLUMN model_pickup_points.model_id IS 'ID модели';
COMMENT ON COLUMN model_pickup_points.pickup_point_id IS 'ID пункта выдачи';
COMMENT ON COLUMN model_pickup_points.is_available IS 'Признак доступности модели в пункте выдачи';
COMMENT ON COLUMN model_pickup_points.updated_at IS 'Время обновления записи';
