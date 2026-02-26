package net.ezpos.console.feature.merchant.dto

import jakarta.validation.constraints.NotBlank

data class CreateMerchantRequest(
    @field:NotBlank
    val name: String,

    val contactName: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val memo: String? = null,
)
