package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "套餐信息")
data class PlanDto(
    @Schema(description = "套餐 ID")
    val id: Long,
    @Schema(description = "套餐名称", example = "基础版-月付")
    val name: String,
    @Schema(description = "套餐描述")
    val description: String? = null,
    @Schema(description = "套餐时长（天）", example = "30")
    val durationDays: Int,
    @Schema(description = "价格（分）", example = "9900")
    val price: Long,
    @Schema(description = "是否启用")
    val enabled: Boolean,
)
