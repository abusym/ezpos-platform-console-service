CREATE TABLE merchants (
    id              BIGINT        NOT NULL,
    name            VARCHAR(128)  NOT NULL,
    contact_name    VARCHAR(64),
    contact_phone   VARCHAR(32),
    address         VARCHAR(512),
    memo            VARCHAR(1000),
    enabled         BOOLEAN       NOT NULL DEFAULT true,
    updated_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_merchants PRIMARY KEY (id)
);

CREATE INDEX idx_merchants_enabled ON merchants (enabled);
CREATE INDEX idx_merchants_created_at ON merchants (created_at);
