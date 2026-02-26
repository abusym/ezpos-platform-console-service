-- =============================================
-- V2: 客户端更新上报表
-- =============================================

CREATE TABLE client_update_reports
(
    id               BIGINT        NOT NULL,
    application_code VARCHAR(64)   NOT NULL,
    platform         VARCHAR(32)   NOT NULL,
    tenant_id        VARCHAR(64)   NOT NULL,
    device_id        VARCHAR(128),
    from_version     VARCHAR(32)   NOT NULL,
    to_version       VARCHAR(32)   NOT NULL,
    status           VARCHAR(16)   NOT NULL,
    error_message    VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_client_update_reports PRIMARY KEY (id)
);

CREATE INDEX idx_client_update_reports_app_platform ON client_update_reports (application_code, platform);
CREATE INDEX idx_client_update_reports_created_at ON client_update_reports (created_at);
