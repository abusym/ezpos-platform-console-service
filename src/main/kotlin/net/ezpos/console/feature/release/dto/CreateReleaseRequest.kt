package net.ezpos.console.feature.release.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import java.time.OffsetDateTime

/**
 * 创建发布配置的请求体（create-only）。
 *
 * 该请求倾向于“提供完整的发布配置快照”，因此关键字段为必填；其余字段按业务含义可为空。
 * 校验约束由 Jakarta Validation 注解提供（例如长度限制、百分比范围等）。
 *
 * 灰度字段的典型约定：
 * - 当 [rolloutType] 为 [ReleaseRolloutType.ALL]：可不提供 [percent]/[whitelistTenants]
 * - 当 [rolloutType] 为 [ReleaseRolloutType.PERCENT]：应提供 [percent]，并可选提供 [rolloutSalt]
 * - 当 [rolloutType] 为 [ReleaseRolloutType.WHITELIST]：应提供 [whitelistTenants]
 */
data class CreateReleaseRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val applicationCode: String,

    @field:NotBlank
    @field:Size(max = 32)
    val platform: String,

    @field:NotBlank
    @field:Size(max = 32)
    val version: String,

    @field:NotBlank
    @field:Size(max = 32)
    val minSupportedVersion: String,

    @field:Size(max = 4000)
    val releaseNotes: String? = null,

    @field:Size(max = 512)
    val artifactKey: String? = null,

    @field:Size(max = 2000)
    val artifactUrl: String? = null,

    @field:Size(max = 128)
    val sha256: String? = null,

    val fileSize: Long? = null,

    val isForced: Boolean = false,

    val forceAfterAt: OffsetDateTime? = null,

    val rolloutType: ReleaseRolloutType = ReleaseRolloutType.ALL,

    @field:Min(0)
    @field:Max(100)
    val percent: Int? = null,

    val whitelistTenants: List<String>? = null,

    @field:Size(max = 64)
    val rolloutSalt: String? = null,
)

