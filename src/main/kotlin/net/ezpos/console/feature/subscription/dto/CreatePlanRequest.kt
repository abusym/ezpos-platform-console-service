package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

@Schema(description = "创建套餐请求")
data class CreatePlanRequest(
    @field:NotBlank
    @Schema(description = "套餐名称", example = "基础版-月付")
    val name: String,

    @Schema(description = "套餐描述", example = "适合小型商户的基础套餐")
    val description: String? = null,

    @field:Positive
    @Schema(description = "套餐时长（天）", example = "30")
    val durationDays: Int,

    @field:PositiveOrZero
    @Schema(description = "价格（分）", example = "9900")
    val price: Long,

    @Schema(description = "是否启用", example = "true")
    val enabled: Boolean = true,
)
