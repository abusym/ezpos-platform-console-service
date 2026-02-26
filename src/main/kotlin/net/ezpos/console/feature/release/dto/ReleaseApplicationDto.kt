package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 可发布应用（Release Application）的对外响应 DTO。
 *
 * @property id 应用记录 id
 * @property code 应用编码（唯一）
 * @property name 应用名称
 * @property description 应用描述（可为空）
 * @property enabled 是否启用
 */
@Schema(description = "应用信息")
data class ReleaseApplicationDto(
    @Schema(description = "应用 ID")
    val id: Long,
    @Schema(description = "应用编码", example = "ezpos-cashier")
    val code: String,
    @Schema(description = "应用名称", example = "EzPos 收银台")
    val name: String,
    @Schema(description = "应用描述")
    val description: String?,
    @Schema(description = "是否启用")
    val enabled: Boolean,
)
