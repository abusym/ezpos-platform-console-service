package net.ezpos.console.feature.release.dto

import jakarta.validation.constraints.Size

/**
 * 局部更新可发布应用（Release Application）的请求体。
 *
 * 所有字段均为可选：字段为 `null` 通常表示“不修改该字段”。
 * 校验约束由 Jakarta Validation 注解提供（主要是长度限制）。
 */
data class UpdateReleaseApplicationRequest(
    @field:Size(max = 128)
    val name: String? = null,

    @field:Size(max = 512)
    val description: String? = null,

    val enabled: Boolean? = null,
)

