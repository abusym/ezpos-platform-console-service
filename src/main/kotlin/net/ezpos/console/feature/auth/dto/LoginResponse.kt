package net.ezpos.console.feature.auth.dto

data class LoginResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val expiresInSeconds: Long,
    val user: PlatformUserInfo,
)

