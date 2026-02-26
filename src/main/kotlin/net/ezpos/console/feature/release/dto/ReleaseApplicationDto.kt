package net.ezpos.console.feature.release.dto

/**
 * 可发布应用（Release Application）的对外响应 DTO。
 *
 * @property id 应用记录 id
 * @property code 应用编码（唯一）
 * @property name 应用名称
 * @property description 应用描述（可为空）
 * @property enabled 是否启用
 */
data class ReleaseApplicationDto(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val enabled: Boolean,
)

