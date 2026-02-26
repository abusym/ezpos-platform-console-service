package net.ezpos.console.feature.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "登录响应")
data class LoginResponse(
    @Schema(description = "令牌类型", example = "Bearer")
    val tokenType: String = "Bearer",
    @Schema(description = "访问令牌")
    val accessToken: String,
    @Schema(description = "令牌有效期（秒）", example = "86400")
    val expiresInSeconds: Long,
    @Schema(description = "当前用户信息")
    val user: PlatformUserInfo,
)
