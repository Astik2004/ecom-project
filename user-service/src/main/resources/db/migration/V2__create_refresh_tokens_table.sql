CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id         UUID          NOT NULL DEFAULT gen_random_uuid(),
    token      VARCHAR(1000) NOT NULL,
    revoked    BOOLEAN       NOT NULL DEFAULT FALSE,
    expired    BOOLEAN       NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP     NOT NULL,
    user_id    UUID          NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    status     VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN       NOT NULL DEFAULT FALSE,
    version    BIGINT                 DEFAULT 0,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user  ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_value ON refresh_tokens (token);