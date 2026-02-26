package net.ezpos.console.feature.release.dto

import io.swagger.v3.oas.annotations.media.Schema
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
@Schema(description = "客户端更新上报请求")
data class ClientUpdateReportRequest(
    @field:NotBlank
    @Schema(description = "应用编码", example = "ezpos-cashier")
    val applicationCode: String,
    @field:NotBlank
    @Schema(description = "平台标识", example = "windows-x64")
    val platform: String,
    @field:NotBlank
    @Schema(description = "租户 ID", example = "T10001")
    val tenantId: String,
    @Schema(description = "设备 ID")
    val deviceId: String? = null,
    @field:NotBlank
    @Schema(description = "更新前版本", example = "1.0.0")
    val fromVersion: String,
    @field:NotBlank
    @Schema(description = "更新目标版本", example = "1.2.0")
    val toVersion: String,
    @field:NotBlank
    @Schema(description = "上报状态（downloaded / installed / failed）", example = "installed")
    val status: String,
    @Schema(description = "错误信息")
    val errorMessage: String? = null,
)
