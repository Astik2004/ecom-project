CREATE TABLE IF NOT EXISTS audit_logs
(
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    action            VARCHAR(50)  NOT NULL,
    entity_name       VARCHAR(255) NOT NULL,
    entity_id         UUID,
    old_values        VARCHAR(4000),
    new_values        VARCHAR(4000),
    changed_fields    VARCHAR(1000),
    performed_by      VARCHAR(255) NOT NULL,
    performed_by_role VARCHAR(30),
    ip_address        VARCHAR(50),
    user_agent        VARCHAR(500),
    request_uri       VARCHAR(500),
    request_method    VARCHAR(10),
    description       VARCHAR(500),
    success           BOOLEAN,
    failure_reason    VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_entity_id    ON audit_logs (entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_action       ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_created_at   ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON audit_logs (performed_by);