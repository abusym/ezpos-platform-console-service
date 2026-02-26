package net.ezpos.console.feature.migration.dto

import java.time.OffsetDateTime

data class DataMigrationDto(
    val id: Long,
    val title: String,
    val description: String? = null,
    val sourceMerchantId: Long? = null,
    val targetMerchantId: Long? = null,
    val type: String,
    val status: String,
    val progress: Int,
    val errorMessage: String? = null,
    val startedAt: OffsetDateTime? = null,
    val completedAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime? = null,
)
