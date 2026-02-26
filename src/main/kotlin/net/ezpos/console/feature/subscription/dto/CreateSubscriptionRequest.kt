package net.ezpos.console.feature.subscription.dto

import jakarta.validation.constraints.NotNull

data class CreateSubscriptionRequest(
    @field:NotNull
    val merchantId: Long,

    @field:NotNull
    val planId: Long,
)
