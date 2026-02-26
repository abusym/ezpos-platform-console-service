package net.ezpos.console.feature.release.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import net.ezpos.console.common.entity.base.IdEntity
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import net.ezpos.console.feature.release.model.ReleaseStatus
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

/**
 * 某应用在某平台上的“当前发布版本”配置。
 *
 * 数据库约束：
 * - `application_code + platform` 唯一，表示同一应用同一平台在任意时刻只维护一条发布记录（可被更新）。
 *
 * 投放/灰度相关字段通常会组合使用：
 * - 当 [rolloutType] 为 [ReleaseRolloutType.ALL]：忽略 [percent] / [whitelistTenants]
 * - 当 [rolloutType] 为 [ReleaseRolloutType.PERCENT]：使用 [percent] 与 [rolloutSalt] 做稳定分流
 * - 当 [rolloutType] 为 [ReleaseRolloutType.WHITELIST]：使用 [whitelistTenants] 判定是否命中
 *
 * @property applicationCode 应用编码（业务主键的一部分）
 * @property platform 平台标识（业务主键的一部分，例如 android/ios/desktop 等）
 * @property version 当前发布的版本号字符串（通常为语义化版本，但不在此处强制）
 * @property minSupportedVersion 最低可支持的版本号（低于该版本的客户端需要强制升级或被阻止使用）
 * @property releaseNotes 发布说明（可为空）
 * @property artifactKey 制品在对象存储中的 key（可为空）
 * @property artifactUrl 制品下载地址（可为空）
 * @property sha256 制品内容的 SHA-256 校验和（可为空）
 * @property fileSize 制品文件大小（字节，可为空）
 * @property isForced 是否强制升级
 * @property forceAfterAt 强制升级生效时间点（可为空；为空时表示立即或由其他逻辑决定）
 * @property rolloutType 投放策略类型
 * @property percent 百分比投放的目标比例（通常 0-100；仅在 [rolloutType] 为 PERCENT 时有意义）
 * @property whitelistTenants 白名单租户集合的编码字符串（仅在 [rolloutType] 为 WHITELIST 时有意义）
 * @property rolloutSalt 稳定分流盐（用于避免同一租户在不同版本间“抖动”）
 * @property status 发布状态（例如发布/暂停）
 * @property publishedAt 最近一次进入“发布”状态的时间（可为空）
 */
@Entity
@Table(
    name = "console_releases",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_console_releases_application_platform",
            columnNames = ["application_code", "platform"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_console_releases_application_platform_status",
            columnList = "application_code, platform, status",
        ),
    ],
)
class Release(
    @Column(name = "application_code", nullable = false, length = 64)
    var applicationCode: String,

    @Column(name = "platform", nullable = false, length = 32)
    var platform: String,

    @Column(name = "version", nullable = false, length = 32)
    var version: String,

    @Column(name = "min_supported_version", nullable = false, length = 32)
    var minSupportedVersion: String,

    @Column(name = "release_notes", nullable = true, length = 4000)
    var releaseNotes: String? = null,

    @Column(name = "artifact_key", nullable = true, length = 512)
    var artifactKey: String? = null,

    @Column(name = "artifact_url", nullable = true, length = 2000)
    var artifactUrl: String? = null,

    @Column(name = "sha256", nullable = true, length = 128)
    var sha256: String? = null,

    @Column(name = "file_size", nullable = true)
    var fileSize: Long? = null,

    @Column(name = "is_forced", nullable = false)
    var isForced: Boolean = false,

    @Column(name = "force_after_at", nullable = true, columnDefinition = "timestamptz")
    var forceAfterAt: OffsetDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "rollout_type", nullable = false, length = 16)
    var rolloutType: ReleaseRolloutType = ReleaseRolloutType.ALL,

    @Column(name = "percent", nullable = true)
    var percent: Int? = null,

    @Column(name = "whitelist_tenants", nullable = true, length = 8000)
    var whitelistTenants: String? = null,

    @Column(name = "rollout_salt", nullable = true, length = 64)
    var rolloutSalt: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: ReleaseStatus = ReleaseStatus.PAUSED,

    @Column(name = "published_at", nullable = true, columnDefinition = "timestamptz")
    var publishedAt: OffsetDateTime? = null,
) : IdEntity() {
    /**
     * 最近一次更新时间（由 Hibernate 自动维护）。
     *
     * 用于审计/缓存失效/对账等场景，业务逻辑不应依赖该字段的精确值来做判定。
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}

