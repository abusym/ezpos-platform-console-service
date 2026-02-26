package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 创建可发布应用（Release Application）的请求体。
 *
 * 校验约束由 Jakarta Validation 注解提供：
 * - `code`/`name` 必填
 * - 字段长度上限与数据库列长度保持一致或更严格
 */
@Schema(description = "创建应用请求")
data class CreateReleaseApplicationRequest(
    @field:NotBlank
    @field:Size(max = 64)
    @Schema(description = "应用编码（唯一）", example = "ezpos-cashier")
    val code: String,

    @field:NotBlank
    @field:Size(max = 128)
    @Schema(description = "应用名称", example = "EzPos 收银台")
    val name: String,

    @field:Size(max = 512)
    @Schema(description = "应用描述")
    val description: String? = null,

    @Schema(description = "是否启用", example = "true")
    val enabled: Boolean = true,
)
