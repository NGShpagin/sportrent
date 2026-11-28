-- Align promocodes with percent-based model and composite PK on promocodes_used.

ALTER TABLE promocodes DROP COLUMN type;

ALTER TABLE promocodes
    ADD COLUMN user_id         uuid,
    ADD COLUMN percent         integer NOT NULL DEFAULT 0,
    ADD COLUMN max_total_users integer;

ALTER TABLE promocodes
    ADD CONSTRAINT promocodes_percent_chk CHECK (percent >= 0 AND percent <= 100);

ALTER TABLE promocodes
    ADD CONSTRAINT promocodes_max_users_chk CHECK (
        max_total_users IS NULL OR max_total_users > 0
    );

COMMENT ON COLUMN promocodes.user_id IS 'ID пользователя. Заполняется для персональных промокодов';
COMMENT ON COLUMN promocodes.percent IS 'Процент скидки';
COMMENT ON COLUMN promocodes.max_total_users IS 'Максимальное количество использования';
COMMENT ON COLUMN promocodes.created_at IS 'Время создания записи';
COMMENT ON COLUMN promocodes.updated_at IS 'Время обновления записи';
COMMENT ON COLUMN promocodes.deleted_at IS 'Время удаления записи';

ALTER TABLE promocodes_used DROP CONSTRAINT promocodes_used_pkey;
ALTER TABLE promocodes_used DROP COLUMN id;
ALTER TABLE promocodes_used
    ADD CONSTRAINT promocodes_used_pkey PRIMARY KEY (promocode_id, user_id);

COMMENT ON COLUMN promocodes_used.created_at IS 'Время использования промокода';
