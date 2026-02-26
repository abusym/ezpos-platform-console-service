package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "续费订阅请求")
data class RenewSubscriptionRequest(
    @Schema(description = "套餐 ID（为空则沿用当前套餐）")
    val planId: Long? = null,
)
