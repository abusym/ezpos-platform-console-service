package net.ezpos.console.feature.release.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import java.time.OffsetDateTime

/**
 * 局部更新发布配置的请求体（Patch/Update 语义）。
 *
 * 所有字段均为可选：字段为 `null` 通常表示“不修改该字段”，非 `null` 表示将其更新为指定值。
 * 校验约束由 Jakarta Validation 注解提供（例如长度限制、百分比范围等）。
 *
 * 灰度字段的典型约定与 [CreateReleaseRequest] 相同；当只更新灰度策略时，业务层应同时校验相关字段组合的完整性。
 */
data class UpdateReleaseRequest(
    @field:Size(max = 32)
    val version: String? = null,

    @field:Size(max = 32)
    val minSupportedVersion: String? = null,

    @field:Size(max = 4000)
    val releaseNotes: String? = null,

    @field:Size(max = 512)
    val artifactKey: String? = null,

    @field:Size(max = 2000)
    val artifactUrl: String? = null,

    @field:Size(max = 128)
    val sha256: String? = null,

    val fileSize: Long? = null,

    val isForced: Boolean? = null,

    val forceAfterAt: OffsetDateTime? = null,

    val rolloutType: ReleaseRolloutType? = null,

    @field:Min(0)
    @field:Max(100)
    val percent: Int? = null,

    val whitelistTenants: List<String>? = null,

    @field:Size(max = 64)
    val rolloutSalt: String? = null,
)

