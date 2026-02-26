package net.ezpos.console.feature.user.dto

data class UpdatePlatformUserRequest(
    val displayName: String? = null,
    val email: String? = null,
    val enabled: Boolean? = null,
)
