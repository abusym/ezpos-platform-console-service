package net.ezpos.console.feature.audit.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "审计日志")
data class AuditLogDto(
    @Schema(description = "日志 ID")
    val id: Long,
    @Schema(description = "操作用户 ID")
    val userId: Long? = null,
    @Schema(description = "操作用户名")
    val username: String? = null,
    @Schema(description = "操作动作", example = "CREATE")
    val action: String,
    @Schema(description = "资源类型", example = "MERCHANT")
    val resourceType: String,
    @Schema(description = "资源 ID")
    val resourceId: String? = null,
    @Schema(description = "操作详情")
    val detail: String? = null,
    @Schema(description = "IP 地址", example = "192.168.1.1")
    val ipAddress: String? = null,
    @Schema(description = "创建时间")
    val createdAt: OffsetDateTime? = null,
)
