package net.ezpos.console.feature.subscription.service

import io.mockk.every
import io.mockk.mockk
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.subscription.dto.CreateSubscriptionRequest
import net.ezpos.console.feature.subscription.dto.RenewSubscriptionRequest
import net.ezpos.console.feature.subscription.entity.Plan
import net.ezpos.console.feature.subscription.entity.Subscription
import net.ezpos.console.feature.subscription.mapper.SubscriptionMapper
import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import net.ezpos.console.feature.subscription.repository.PlanRepository
import net.ezpos.console.feature.subscription.repository.SubscriptionRepository
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SubscriptionServiceTest {

    private val repo = mockk<SubscriptionRepository>(relaxed = true)
    private val planRepo = mockk<PlanRepository>()
    private val mapper = mockk<SubscriptionMapper>()
    private val service = SubscriptionService(repo, planRepo, mapper)

    // -- create --

    @Test
    fun `create sets startDate to today and endDate to today plus durationDays`() {
        val plan = aPlan()
        every { planRepo.findById(1L) } returns Optional.of(plan)
        every { repo.save(any()) } answers { firstArg<Subscription>().apply { id = 10L } }
        every { mapper.toDto(10L, any()) } returns mockk()

        service.create(CreateSubscriptionRequest(merchantId = 100L, planId = 1L))

        io.mockk.verify {
            repo.save(match<Subscription> {
                it.merchantId == 100L &&
                    it.planId == 1L &&
                    it.startDate == LocalDate.now() &&
                    it.endDate == LocalDate.now().plusDays(30) &&
                    it.status == SubscriptionStatus.ACTIVE
            })
        }
    }

    @Test
    fun `create throws EntityNotFoundException when plan not found`() {
        every { planRepo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.create(CreateSubscriptionRequest(merchantId = 100L, planId = 999L))
        }
    }

    // -- renew --

    @Test
    fun `renew extends endDate by plan durationDays`() {
        val subscription = aSubscription()
        val plan = aPlan()
        every { repo.findById(10L) } returns Optional.of(subscription)
        every { planRepo.findById(1L) } returns Optional.of(plan)
        every { repo.save(subscription) } returns subscription
        every { mapper.toDto(10L, subscription) } returns mockk()

        service.renew(10L, RenewSubscriptionRequest())

        assertEquals(LocalDate.now().plusDays(60), subscription.endDate)
        assertNotNull(subscription.renewedAt)
    }

    @Test
    fun `renew with different plan updates planId`() {
        val subscription = aSubscription()
        val newPlan = Plan(name = "Premium", durationDays = 60, price = 19900).apply { id = 2L }
        every { repo.findById(10L) } returns Optional.of(subscription)
        every { planRepo.findById(2L) } returns Optional.of(newPlan)
        every { repo.save(subscription) } returns subscription
        every { mapper.toDto(10L, subscription) } returns mockk()

        service.renew(10L, RenewSubscriptionRequest(planId = 2L))

        assertEquals(2L, subscription.planId)
        assertEquals(LocalDate.now().plusDays(90), subscription.endDate)
    }

    @Test
    fun `renew throws EntityNotFoundException when subscription not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.renew(999L, RenewSubscriptionRequest())
        }
    }

    // -- listExpiring --

    @Test
    fun `listExpiring delegates to repository with correct parameters`() {
        every { repo.findByEndDateBeforeAndStatus(any(), SubscriptionStatus.ACTIVE) } returns emptyList()

        service.listExpiring(15)

        io.mockk.verify {
            repo.findByEndDateBeforeAndStatus(LocalDate.now().plusDays(15), SubscriptionStatus.ACTIVE)
        }
    }

    private fun aPlan() = Plan(
        name = "Basic",
        description = "Basic plan",
        durationDays = 30,
        price = 9900,
        enabled = true,
    ).apply { id = 1L }

    private fun aSubscription() = Subscription(
        merchantId = 100L,
        planId = 1L,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(30),
        status = SubscriptionStatus.ACTIVE,
    ).apply { id = 10L }
}
