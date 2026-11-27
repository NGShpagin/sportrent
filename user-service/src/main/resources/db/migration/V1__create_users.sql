CREATE TABLE users (
    id          uuid         NOT NULL,
    username    varchar(50)  NOT NULL,
    password    varchar(255) NOT NULL,
    first_name  varchar(50)  NOT NULL,
    last_name   varchar(50)  NOT NULL,
    middle_name varchar(50),
    email       varchar(50),
    birth_date  date,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz,
    deleted_at  timestamptz,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);
