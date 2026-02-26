package net.ezpos.console.feature.merchant.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "更新商家请求")
data class UpdateMerchantRequest(
    @Schema(description = "商家名称", example = "星巴克咖啡")
    val name: String? = null,
    @Schema(description = "联系人姓名", example = "张三")
    val contactName: String? = null,
    @Schema(description = "联系电话", example = "13800138000")
    val contactPhone: String? = null,
    @Schema(description = "地址", example = "上海市浦东新区xx路xx号")
    val address: String? = null,
    @Schema(description = "备注")
    val memo: String? = null,
)
