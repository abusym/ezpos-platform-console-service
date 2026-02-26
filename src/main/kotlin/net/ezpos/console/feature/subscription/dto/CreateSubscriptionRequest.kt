package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "创建订阅请求")
data class CreateSubscriptionRequest(
    @field:NotNull
    @Schema(description = "商家 ID")
    val merchantId: Long,

    @field:NotNull
    @Schema(description = "套餐 ID")
    val planId: Long,
)
