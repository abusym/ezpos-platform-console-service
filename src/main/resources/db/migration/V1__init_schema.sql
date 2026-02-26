-- =============================================
-- V1: 初始化 Schema
-- 创建 platform_users / console_release_applications / console_releases 三张表
-- =============================================

CREATE TABLE platform_users
(
    id            BIGINT       NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100),
    display_name  VARCHAR(128),
    email         VARCHAR(128),
    enabled       BOOLEAN      NOT NULL DEFAULT true,
    updated_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_platform_users PRIMARY KEY (id),
    CONSTRAINT uk_platform_users_username UNIQUE (username)
);

CREATE INDEX idx_platform_users_username ON platform_users (username);

-- ---

CREATE TABLE console_release_applications
(
    id          BIGINT       NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_console_release_applications PRIMARY KEY (id),
    CONSTRAINT uk_console_release_applications_code UNIQUE (code)
);

CREATE INDEX idx_console_release_applications_code ON console_release_applications (code);
CREATE INDEX idx_console_release_applications_enabled ON console_release_applications (enabled);

-- ---

CREATE TABLE console_releases
(
    id                    BIGINT       NOT NULL,
    application_code      VARCHAR(64)  NOT NULL,
    platform              VARCHAR(32)  NOT NULL,
    version               VARCHAR(32)  NOT NULL,
    min_supported_version VARCHAR(32)  NOT NULL,
    release_notes         VARCHAR(4000),
    artifact_key          VARCHAR(512),
    artifact_url          VARCHAR(2000),
    sha256                VARCHAR(128),
    file_size             BIGINT,
    is_forced             BOOLEAN      NOT NULL DEFAULT false,
    force_after_at        TIMESTAMPTZ,
    rollout_type          VARCHAR(16)  NOT NULL DEFAULT 'ALL',
    percent               INTEGER,
    whitelist_tenants     VARCHAR(8000),
    rollout_salt          VARCHAR(64),
    status                VARCHAR(16)  NOT NULL DEFAULT 'PAUSED',
    published_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_console_releases PRIMARY KEY (id),
    CONSTRAINT uk_console_releases_application_platform UNIQUE (application_code, platform)
);

CREATE INDEX idx_console_releases_application_platform_status
    ON console_releases (application_code, platform, status);
