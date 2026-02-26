package net.ezpos.console.feature.subscription.dto

data class UpdatePlanRequest(
    val name: String? = null,
    val description: String? = null,
    val durationDays: Int? = null,
    val price: Long? = null,
    val enabled: Boolean? = null,
)
