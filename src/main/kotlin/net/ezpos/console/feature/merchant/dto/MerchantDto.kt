package net.ezpos.console.feature.merchant.dto

import java.time.OffsetDateTime

data class MerchantDto(
    val id: Long,
    val name: String,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val memo: String? = null,
    val enabled: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
