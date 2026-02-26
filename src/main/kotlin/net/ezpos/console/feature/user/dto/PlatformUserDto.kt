package net.ezpos.console.feature.user.dto

data class PlatformUserDto(
    val id: Long,
    val username: String,
    val displayName: String? = null,
    val email: String? = null,
    val enabled: Boolean,
)

