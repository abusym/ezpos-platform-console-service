package net.ezpos.console.feature.release.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 创建可发布应用（Release Application）的请求体。
 *
 * 校验约束由 Jakarta Validation 注解提供：
 * - `code`/`name` 必填
 * - 字段长度上限与数据库列长度保持一致或更严格
 */
data class CreateReleaseApplicationRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val code: String,

    @field:NotBlank
    @field:Size(max = 128)
    val name: String,

    @field:Size(max = 512)
    val description: String? = null,

    val enabled: Boolean = true,
)

