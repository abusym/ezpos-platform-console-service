package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "更新套餐请求")
data class UpdatePlanRequest(
    @Schema(description = "套餐名称", example = "基础版-月付")
    val name: String? = null,
    @Schema(description = "套餐描述")
    val description: String? = null,
    @Schema(description = "套餐时长（天）", example = "30")
    val durationDays: Int? = null,
    @Schema(description = "价格（分）", example = "9900")
    val price: Long? = null,
    @Schema(description = "是否启用")
    val enabled: Boolean? = null,
)
