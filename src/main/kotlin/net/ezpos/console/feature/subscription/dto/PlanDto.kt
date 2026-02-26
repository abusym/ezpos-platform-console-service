package net.ezpos.console.feature.subscription.dto

data class PlanDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val durationDays: Int,
    val price: Long,
    val enabled: Boolean,
)
