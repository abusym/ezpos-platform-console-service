package net.ezpos.console.feature.user.dto

import jakarta.validation.constraints.NotBlank

data class CreatePlatformUserRequest(
    @field:NotBlank
    val username: String,

    @field:NotBlank
    val password: String,

    val displayName: String? = null,
    val email: String? = null,
    val enabled: Boolean = true,
)

