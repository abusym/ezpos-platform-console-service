package net.ezpos.console.feature.merchant.mapper

import net.ezpos.console.feature.merchant.dto.MerchantDto
import net.ezpos.console.feature.merchant.entity.Merchant
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MerchantMapper {
    fun toDto(id: Long, merchant: Merchant): MerchantDto
}
