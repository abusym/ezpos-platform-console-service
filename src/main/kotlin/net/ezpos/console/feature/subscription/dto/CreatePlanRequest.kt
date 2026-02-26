package net.ezpos.console.feature.subscription.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class CreatePlanRequest(
    @field:NotBlank
    val name: String,

    val description: String? = null,

    @field:Positive
    val durationDays: Int,

    @field:PositiveOrZero
    val price: Long,

    val enabled: Boolean = true,
)
