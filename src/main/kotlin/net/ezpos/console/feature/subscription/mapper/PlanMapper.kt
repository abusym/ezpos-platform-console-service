package net.ezpos.console.feature.subscription.mapper

import net.ezpos.console.feature.subscription.dto.PlanDto
import net.ezpos.console.feature.subscription.entity.Plan
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface PlanMapper {
    fun toDto(id: Long, plan: Plan): PlanDto
}
