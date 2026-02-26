package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 局部更新可发布应用（Release Application）的请求体。
 *
 * 所有字段均为可选：字段为 `null` 通常表示"不修改该字段"。
 * 校验约束由 Jakarta Validation 注解提供（主要是长度限制）。
 */
@Schema(description = "更新应用请求")
data class UpdateReleaseApplicationRequest(
    @field:Size(max = 128)
    @Schema(description = "应用名称", example = "EzPos 收银台")
    val name: String? = null,

    @field:Size(max = 512)
    @Schema(description = "应用描述")
    val description: String? = null,

    @Schema(description = "是否启用")
    val enabled: Boolean? = null,
)
