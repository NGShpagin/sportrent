CREATE TABLE sessions (
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    status     varchar(20) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    CONSTRAINT sessions_pkey PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens (
    id         uuid         NOT NULL,
    user_id    uuid         NOT NULL,
    token      varchar(200) NOT NULL,
    expires_at timestamptz  NOT NULL,
    status     varchar(20)  NOT NULL,
    session_id uuid         NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_session
        FOREIGN KEY (session_id) REFERENCES sessions (id)
);

CREATE UNIQUE INDEX idx_rt_token ON refresh_tokens (token);
CREATE INDEX idx_rt_userid ON refresh_tokens (user_id);
