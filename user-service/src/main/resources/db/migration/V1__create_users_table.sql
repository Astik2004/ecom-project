CREATE TABLE IF NOT EXISTS users
(
    id                              UUID         NOT NULL DEFAULT gen_random_uuid(),
    first_name                      VARCHAR(50)  NOT NULL,
    last_name                       VARCHAR(50)  NOT NULL,
    email                           VARCHAR(150) NOT NULL,
    password                        VARCHAR(255) NOT NULL,
    phone_number                    VARCHAR(15),
    role                            VARCHAR(20)  NOT NULL,
    status                          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    profile_picture_url             TEXT,
    is_email_verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_phone_verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted                      BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at                   TIMESTAMP,
    failed_login_attempts           INTEGER      NOT NULL DEFAULT 0,
    account_locked_until            TIMESTAMP,
    password_changed_at             TIMESTAMP,
    email_verification_token        VARCHAR(255),
    email_verification_token_expiry TIMESTAMP,
    password_reset_token            VARCHAR(255),
    password_reset_token_expiry     TIMESTAMP,
    created_at                      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP,
    created_by                      VARCHAR(255),
    updated_by                      VARCHAR(255),
    version                         BIGINT                DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone_number)
);

CREATE INDEX IF NOT EXISTS idx_user_email   ON users (email);
CREATE INDEX IF NOT EXISTS idx_user_phone   ON users (phone_number);
CREATE INDEX IF NOT EXISTS idx_user_status  ON users (status);
CREATE INDEX IF NOT EXISTS idx_user_role    ON users (role);
CREATE INDEX IF NOT EXISTS idx_user_created ON users (created_at);