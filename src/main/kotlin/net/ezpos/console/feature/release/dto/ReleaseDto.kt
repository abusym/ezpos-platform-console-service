package net.ezpos.console.feature.release.dto

import net.ezpos.console.feature.release.model.ReleaseRolloutType
import net.ezpos.console.feature.release.model.ReleaseStatus
import java.time.OffsetDateTime

/**
 * 发布配置的对外响应 DTO。
 *
 * 通常用于控制台管理接口（CRUD）返回，以及在需要展示当前发布配置详情的场景中复用。
 *
 * @property id 发布记录 id
 * @property applicationCode 应用编码
 * @property platform 平台标识
 * @property version 当前发布版本号
 * @property minSupportedVersion 最低支持版本号
 * @property releaseNotes 发布说明（可为空）
 * @property artifactKey 制品 key（可为空）
 * @property artifactUrl 制品下载地址（可为空）
 * @property sha256 制品 SHA-256（可为空）
 * @property fileSize 文件大小（字节，可为空）
 * @property isForced 是否强制升级
 * @property forceAfterAt 强制升级生效时间（可为空）
 * @property rolloutType 投放策略类型
 * @property percent 百分比投放值（仅在 [rolloutType] 为 PERCENT 时有意义）
 * @property whitelistTenants 白名单租户列表（仅在 [rolloutType] 为 WHITELIST 时有意义；未配置时通常为空列表）
 * @property rolloutSalt 稳定分流盐（可为空）
 * @property status 发布状态
 * @property publishedAt 发布生效时间（可为空）
 * @property createdAt 创建时间（可为空，取决于实体基类是否维护）
 * @property updatedAt 更新时间（可为空）
 */
data class ReleaseDto(
    val id: Long,
    val applicationCode: String,
    val platform: String,
    val version: String,
    val minSupportedVersion: String,
    val releaseNotes: String?,
    val artifactKey: String?,
    val artifactUrl: String?,
    val sha256: String?,
    val fileSize: Long?,
    val isForced: Boolean,
    val forceAfterAt: OffsetDateTime?,
    val rolloutType: ReleaseRolloutType,
    val percent: Int?,
    val whitelistTenants: List<String>,
    val rolloutSalt: String?,
    val status: ReleaseStatus,
    val publishedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

