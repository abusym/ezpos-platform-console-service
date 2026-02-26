package net.ezpos.console.feature.migration.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "创建数据迁移请求")
data class CreateDataMigrationRequest(
    @field:NotBlank
    @Schema(description = "迁移任务标题", example = "商品数据迁移")
    val title: String,

    @Schema(description = "任务描述")
    val description: String? = null,
    @Schema(description = "源商家 ID")
    val sourceMerchantId: Long? = null,
    @Schema(description = "目标商家 ID")
    val targetMerchantId: Long? = null,

    @field:NotNull
    @Schema(description = "迁移类型（product / category / member / full）", example = "product")
    val type: String,
)
