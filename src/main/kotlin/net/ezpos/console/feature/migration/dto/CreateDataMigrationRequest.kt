package net.ezpos.console.feature.migration.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateDataMigrationRequest(
    @field:NotBlank
    val title: String,

    val description: String? = null,
    val sourceMerchantId: Long? = null,
    val targetMerchantId: Long? = null,

    @field:NotNull
    val type: String,
)
