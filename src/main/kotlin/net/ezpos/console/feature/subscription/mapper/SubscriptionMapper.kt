package net.ezpos.console.feature.subscription.mapper

import net.ezpos.console.feature.subscription.dto.SubscriptionDto
import net.ezpos.console.feature.subscription.entity.Subscription
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface SubscriptionMapper {
    fun toDto(id: Long, subscription: Subscription): SubscriptionDto
}
