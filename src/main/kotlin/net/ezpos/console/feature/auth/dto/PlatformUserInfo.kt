package net.ezpos.console.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "平台用户信息")
data class PlatformUserInfo(
    @Schema(description = "用户 ID")
    val id: Long,
    @Schema(description = "用户名", example = "admin")
    val username: String,
    @Schema(description = "显示名称", example = "管理员")
    val displayName: String? = null,
    @Schema(description = "邮箱", example = "admin@ezpos.net")
    val email: String? = null,
    @Schema(description = "是否启用")
    val enabled: Boolean,
)
