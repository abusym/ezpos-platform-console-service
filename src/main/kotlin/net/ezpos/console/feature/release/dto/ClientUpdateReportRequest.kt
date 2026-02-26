package net.ezpos.console.feature.release.dto

import jakarta.validation.constraints.NotBlank

/**
 * 客户端更新上报请求体。
 *
 * @property applicationCode 应用编码
 * @property platform 平台标识
 * @property tenantId 租户 id
 * @property deviceId 设备 id（可选）
 * @property fromVersion 更新前版本
 * @property toVersion 更新目标版本
 * @property status 上报状态（downloaded / installed / failed）
 * @property errorMessage 失败时的错误信息（可选）
 */
data class ClientUpdateReportRequest(
    @field:NotBlank val applicationCode: String,
    @field:NotBlank val platform: String,
    @field:NotBlank val tenantId: String,
    val deviceId: String? = null,
    @field:NotBlank val fromVersion: String,
    @field:NotBlank val toVersion: String,
    @field:NotBlank val status: String,
    val errorMessage: String? = null,
)
