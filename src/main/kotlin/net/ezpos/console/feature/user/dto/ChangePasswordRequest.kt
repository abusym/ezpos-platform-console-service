package net.ezpos.console.feature.user.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    @field:NotBlank
    val oldPassword: String,

    @field:NotBlank
    val newPassword: String,
)
