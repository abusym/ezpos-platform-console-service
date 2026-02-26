package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
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
@Schema(description = "发布配置")
data class ReleaseDto(
    @Schema(description = "发布记录 ID")
    val id: Long,
    @Schema(description = "应用编码", example = "ezpos-cashier")
    val applicationCode: String,
    @Schema(description = "平台标识", example = "windows-x64")
    val platform: String,
    @Schema(description = "版本号", example = "1.2.0")
    val version: String,
    @Schema(description = "最低支持版本号", example = "1.0.0")
    val minSupportedVersion: String,
    @Schema(description = "发布说明")
    val releaseNotes: String?,
    @Schema(description = "制品存储 key")
    val artifactKey: String?,
    @Schema(description = "制品下载地址")
    val artifactUrl: String?,
    @Schema(description = "制品 SHA-256 校验和")
    val sha256: String?,
    @Schema(description = "文件大小（字节）")
    val fileSize: Long?,
    @Schema(description = "是否强制升级")
    val isForced: Boolean,
    @Schema(description = "强制升级生效时间")
    val forceAfterAt: OffsetDateTime?,
    @Schema(description = "灰度策略类型", example = "all")
    val rolloutType: ReleaseRolloutType,
    @Schema(description = "百分比投放值（0-100）")
    val percent: Int?,
    @Schema(description = "白名单租户列表")
    val whitelistTenants: List<String>,
    @Schema(description = "灰度分流盐值")
    val rolloutSalt: String?,
    @Schema(description = "发布状态", example = "published")
    val status: ReleaseStatus,
    @Schema(description = "发布生效时间")
    val publishedAt: OffsetDateTime?,
    @Schema(description = "创建时间")
    val createdAt: OffsetDateTime?,
    @Schema(description = "更新时间")
    val updatedAt: OffsetDateTime?,
)
