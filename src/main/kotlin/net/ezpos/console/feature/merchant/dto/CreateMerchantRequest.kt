package net.ezpos.console.feature.merchant.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "创建商家请求")
data class CreateMerchantRequest(
    @field:NotBlank
    @Schema(description = "商家名称", example = "星巴克咖啡")
    val name: String,

    @Schema(description = "联系人姓名", example = "张三")
    val contactName: String? = null,
    @Schema(description = "联系电话", example = "13800138000")
    val contactPhone: String? = null,
    @Schema(description = "地址", example = "上海市浦东新区xx路xx号")
    val address: String? = null,
    @Schema(description = "备注")
    val memo: String? = null,
)
