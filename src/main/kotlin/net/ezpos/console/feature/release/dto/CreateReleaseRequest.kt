package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import java.time.OffsetDateTime

/**
 * 创建发布配置的请求体（create-only）。
 *
 * 该请求倾向于"提供完整的发布配置快照"，因此关键字段为必填；其余字段按业务含义可为空。
 * 校验约束由 Jakarta Validation 注解提供（例如长度限制、百分比范围等）。
 *
 * 灰度字段的典型约定：
 * - 当 [rolloutType] 为 [ReleaseRolloutType.ALL]：可不提供 [percent]/[whitelistTenants]
 * - 当 [rolloutType] 为 [ReleaseRolloutType.PERCENT]：应提供 [percent]，并可选提供 [rolloutSalt]
 * - 当 [rolloutType] 为 [ReleaseRolloutType.WHITELIST]：应提供 [whitelistTenants]
 */
@Schema(description = "创建发布配置请求")
data class CreateReleaseRequest(
    @field:NotBlank
    @field:Size(max = 64)
    @Schema(description = "应用编码", example = "ezpos-cashier")
    val applicationCode: String,

    @field:NotBlank
    @field:Size(max = 32)
    @Schema(description = "平台标识", example = "windows-x64")
    val platform: String,

    @field:NotBlank
    @field:Size(max = 32)
    @Schema(description = "版本号", example = "1.2.0")
    val version: String,

    @field:NotBlank
    @field:Size(max = 32)
    @Schema(description = "最低支持版本号", example = "1.0.0")
    val minSupportedVersion: String,

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

    @Schema(description = "是否强制升级", example = "false")
    val isForced: Boolean = false,

    @Schema(description = "强制升级生效时间")
    val forceAfterAt: OffsetDateTime? = null,

    @Schema(description = "灰度策略类型", example = "all")
    val rolloutType: ReleaseRolloutType = ReleaseRolloutType.ALL,

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
