package net.ezpos.console.feature.subscription.dto

import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class SubscriptionDto(
    val id: Long,
    val merchantId: Long,
    val planId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: SubscriptionStatus,
    val renewedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime?,
)
