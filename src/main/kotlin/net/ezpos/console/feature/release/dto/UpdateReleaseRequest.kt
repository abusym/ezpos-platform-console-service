package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import java.time.OffsetDateTime

/**
 * 局部更新发布配置的请求体（Patch/Update 语义）。
 *
 * 所有字段均为可选：字段为 `null` 通常表示"不修改该字段"，非 `null` 表示将其更新为指定值。
 * 校验约束由 Jakarta Validation 注解提供（例如长度限制、百分比范围等）。
 *
 * 灰度字段的典型约定与 [CreateReleaseRequest] 相同；当只更新灰度策略时，业务层应同时校验相关字段组合的完整性。
 */
@Schema(description = "更新发布配置请求")
data class UpdateReleaseRequest(
    @field:Size(max = 32)
    @Schema(description = "版本号", example = "1.2.0")
    val version: String? = null,

    @field:Size(max = 32)
    @Schema(description = "最低支持版本号", example = "1.0.0")
    val minSupportedVersion: String? = null,

    @field:Size(max = 4000)
    @Schema(description = "发布说明")
    val releaseNotes: String? = null,

    @field:Size(max = 512)
    @Schema(description = "制品存储 key")
    val artifactKey: String? = null,

    @field:Size(max = 2000)
    @Schema(description = "制品下载地址")
    val artifactUrl: String? = null,

    @field:Size(max = 128)
    @Schema(description = "制品 SHA-256 校验和")
    val sha256: String? = null,

    @Schema(description = "文件大小（字节）")
    val fileSize: Long? = null,

    @Schema(description = "是否强制升级")
    val isForced: Boolean? = null,

    @Schema(description = "强制升级生效时间")
    val forceAfterAt: OffsetDateTime? = null,

    @Schema(description = "灰度策略类型", example = "all")
    val rolloutType: ReleaseRolloutType? = null,

    @field:Min(0)
    @field:Max(100)
    @Schema(description = "百分比投放值（0-100）", example = "50")
    val percent: Int? = null,

    @Schema(description = "白名单租户列表")
    val whitelistTenants: List<String>? = null,

    @field:Size(max = 64)
    @Schema(description = "灰度分流盐值")
    val rolloutSalt: String? = null,
)
