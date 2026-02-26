package net.ezpos.console.feature.subscription.service

import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.subscription.dto.CreateSubscriptionRequest
import net.ezpos.console.feature.subscription.dto.RenewSubscriptionRequest
import net.ezpos.console.feature.subscription.dto.SubscriptionDto
import net.ezpos.console.feature.subscription.entity.Subscription
import net.ezpos.console.feature.subscription.mapper.SubscriptionMapper
import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import net.ezpos.console.feature.subscription.repository.PlanRepository
import net.ezpos.console.feature.subscription.repository.SubscriptionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class SubscriptionService(
    private val repo: SubscriptionRepository,
    private val planRepo: PlanRepository,
    private val mapper: SubscriptionMapper,
) {
    fun create(request: CreateSubscriptionRequest): SubscriptionDto {
        val plan = planRepo.findById(request.planId)
            .orElseThrow { EntityNotFoundException("Plan", request.planId) }

        val today = LocalDate.now()
        val subscription = Subscription(
            merchantId = request.merchantId,
            planId = requireNotNull(plan.id),
            startDate = today,
            endDate = today.plusDays(plan.durationDays.toLong()),
            status = SubscriptionStatus.ACTIVE,
        )

        val saved = repo.save(subscription)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun list(pageable: Pageable): Page<SubscriptionDto> =
        repo.findAll(pageable).map { mapper.toDto(requireNotNull(it.id), it) }

    fun renew(id: Long, request: RenewSubscriptionRequest): SubscriptionDto {
        val subscription = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Subscription", id) }

        val planId = request.planId ?: subscription.planId
        val plan = planRepo.findById(planId)
            .orElseThrow { EntityNotFoundException("Plan", planId) }

        subscription.endDate = subscription.endDate.plusDays(plan.durationDays.toLong())
        subscription.renewedAt = OffsetDateTime.now()
        if (request.planId != null) {
            subscription.planId = request.planId
        }

        val saved = repo.save(subscription)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun listExpiring(days: Int = 30): List<SubscriptionDto> {
        val cutoff = LocalDate.now().plusDays(days.toLong())
        return repo.findByEndDateBeforeAndStatus(cutoff, SubscriptionStatus.ACTIVE)
            .map { mapper.toDto(requireNotNull(it.id), it) }
    }
}
