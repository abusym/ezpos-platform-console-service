package net.ezpos.console.feature.subscription.service

import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.subscription.dto.CreatePlanRequest
import net.ezpos.console.feature.subscription.dto.PlanDto
import net.ezpos.console.feature.subscription.dto.UpdatePlanRequest
import net.ezpos.console.feature.subscription.entity.Plan
import net.ezpos.console.feature.subscription.mapper.PlanMapper
import net.ezpos.console.feature.subscription.repository.PlanRepository
import org.springframework.stereotype.Service

@Service
class PlanService(
    private val repo: PlanRepository,
    private val mapper: PlanMapper,
) {
    fun create(request: CreatePlanRequest): PlanDto {
        val plan = Plan(
            name = request.name,
            description = request.description,
            durationDays = request.durationDays,
            price = request.price,
            enabled = request.enabled,
        )
        val saved = repo.save(plan)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun list(): List<PlanDto> =
        repo.findAll().map { mapper.toDto(requireNotNull(it.id), it) }

    fun update(id: Long, request: UpdatePlanRequest): PlanDto {
        val plan = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Plan", id) }

        request.name?.let { plan.name = it }
        request.description?.let { plan.description = it }
        request.durationDays?.let { plan.durationDays = it }
        request.price?.let { plan.price = it }
        request.enabled?.let { plan.enabled = it }

        val saved = repo.save(plan)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }
}
