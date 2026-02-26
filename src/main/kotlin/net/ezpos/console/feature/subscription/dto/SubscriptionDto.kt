package net.ezpos.console.feature.subscription.dto

import io.swagger.v3.oas.annotations.media.Schema
import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "订阅信息")
data class SubscriptionDto(
    @Schema(description = "订阅 ID")
    val id: Long,
    @Schema(description = "商家 ID")
    val merchantId: Long,
    @Schema(description = "套餐 ID")
    val planId: Long,
    @Schema(description = "开始日期")
    val startDate: LocalDate,
    @Schema(description = "结束日期")
    val endDate: LocalDate,
    @Schema(description = "订阅状态", example = "active")
    val status: SubscriptionStatus,
    @Schema(description = "续费时间")
    val renewedAt: OffsetDateTime?,
    @Schema(description = "创建时间")
    val createdAt: OffsetDateTime?,
)
