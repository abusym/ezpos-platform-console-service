package net.ezpos.console.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "登录请求")
data class LoginRequest(
    @field:NotBlank
    @Schema(description = "用户名", example = "admin")
    val username: String,
    @field:NotBlank
    @Schema(description = "密码", example = "123456")
    val password: String,
)
