package net.ezpos.console.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "更新平台用户请求")
data class UpdatePlatformUserRequest(
    @Schema(description = "显示名称", example = "张三")
    val displayName: String? = null,
    @Schema(description = "邮箱", example = "zhangsan@ezpos.net")
    val email: String? = null,
    @Schema(description = "是否启用")
    val enabled: Boolean? = null,
)
