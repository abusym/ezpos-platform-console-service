package net.ezpos.console.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "修改密码请求")
data class ChangePasswordRequest(
    @field:NotBlank
    @Schema(description = "旧密码")
    val oldPassword: String,

    @field:NotBlank
    @Schema(description = "新密码")
    val newPassword: String,
)
