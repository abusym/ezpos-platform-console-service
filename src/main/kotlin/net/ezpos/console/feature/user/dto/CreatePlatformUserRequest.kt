package net.ezpos.console.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "创建平台用户请求")
data class CreatePlatformUserRequest(
    @field:NotBlank
    @Schema(description = "用户名", example = "zhangsan")
    val username: String,

    @field:NotBlank
    @Schema(description = "密码", example = "password123")
    val password: String,

    @Schema(description = "显示名称", example = "张三")
    val displayName: String? = null,
    @Schema(description = "邮箱", example = "zhangsan@ezpos.net")
    val email: String? = null,
    @Schema(description = "是否启用", example = "true")
    val enabled: Boolean = true,
)
