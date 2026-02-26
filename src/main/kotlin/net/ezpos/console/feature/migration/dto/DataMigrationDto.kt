package net.ezpos.console.feature.migration.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "数据迁移信息")
data class DataMigrationDto(
    @Schema(description = "迁移任务 ID")
    val id: Long,
    @Schema(description = "任务标题", example = "商品数据迁移")
    val title: String,
    @Schema(description = "任务描述")
    val description: String? = null,
    @Schema(description = "源商家 ID")
    val sourceMerchantId: Long? = null,
    @Schema(description = "目标商家 ID")
    val targetMerchantId: Long? = null,
    @Schema(description = "迁移类型", example = "product")
    val type: String,
    @Schema(description = "任务状态（pending / running / completed / failed）", example = "completed")
    val status: String,
    @Schema(description = "进度百分比（0-100）", example = "100")
    val progress: Int,
    @Schema(description = "错误信息")
    val errorMessage: String? = null,
    @Schema(description = "开始时间")
    val startedAt: OffsetDateTime? = null,
    @Schema(description = "完成时间")
    val completedAt: OffsetDateTime? = null,
    @Schema(description = "更新时间")
    val updatedAt: OffsetDateTime? = null,
    @Schema(description = "创建时间")
    val createdAt: OffsetDateTime? = null,
)
