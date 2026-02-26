package net.ezpos.console.feature.auth.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

