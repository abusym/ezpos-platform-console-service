package net.ezpos.console.feature.merchant.dto

data class UpdateMerchantRequest(
    val name: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
    val memo: String? = null,
)
