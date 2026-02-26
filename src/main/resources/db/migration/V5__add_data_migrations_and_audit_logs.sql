CREATE TABLE data_migrations (
    id              BIGINT        NOT NULL,
    title           VARCHAR(128)  NOT NULL,
    description     VARCHAR(1000),
    source_merchant_id  BIGINT,
    target_merchant_id  BIGINT,
    type            VARCHAR(32)   NOT NULL,
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    progress        INTEGER       NOT NULL DEFAULT 0,
    error_message   VARCHAR(2000),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_data_migrations PRIMARY KEY (id)
);

CREATE INDEX idx_data_migrations_status ON data_migrations (status);
CREATE INDEX idx_data_migrations_created_at ON data_migrations (created_at);

CREATE TABLE audit_logs (
    id              BIGINT        NOT NULL,
    user_id         BIGINT,
    username        VARCHAR(64),
    action          VARCHAR(64)   NOT NULL,
    resource_type   VARCHAR(64)   NOT NULL,
    resource_id     VARCHAR(128),
    detail          VARCHAR(2000),
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs (resource_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
