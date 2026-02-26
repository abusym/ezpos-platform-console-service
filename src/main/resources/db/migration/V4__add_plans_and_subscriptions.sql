CREATE TABLE plans (
    id              BIGINT        NOT NULL,
    name            VARCHAR(128)  NOT NULL,
    description     VARCHAR(512),
    duration_days   INTEGER       NOT NULL,
    price           BIGINT        NOT NULL,
    enabled         BOOLEAN       NOT NULL DEFAULT true,
    updated_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_plans PRIMARY KEY (id)
);

CREATE TABLE subscriptions (
    id              BIGINT        NOT NULL,
    merchant_id     BIGINT        NOT NULL,
    plan_id         BIGINT        NOT NULL,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    renewed_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE INDEX idx_subscriptions_merchant_id ON subscriptions (merchant_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions (end_date);
