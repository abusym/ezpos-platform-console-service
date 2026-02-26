package net.ezpos.console.feature.auth.dto

data class PlatformUserInfo(
    val id: Long,
    val username: String,
    val displayName: String? = null,
    val email: String? = null,
    val enabled: Boolean,
)

