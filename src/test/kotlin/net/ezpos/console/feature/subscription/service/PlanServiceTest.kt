package net.ezpos.console.feature.subscription.service

import io.mockk.every
import io.mockk.mockk
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.subscription.dto.CreatePlanRequest
import net.ezpos.console.feature.subscription.dto.UpdatePlanRequest
import net.ezpos.console.feature.subscription.entity.Plan
import net.ezpos.console.feature.subscription.mapper.PlanMapper
import net.ezpos.console.feature.subscription.repository.PlanRepository
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanServiceTest {

    private val repo = mockk<PlanRepository>(relaxed = true)
    private val mapper = mockk<PlanMapper>()
    private val service = PlanService(repo, mapper)

    // -- create --

    @Test
    fun `create saves plan and returns dto`() {
        val request = CreatePlanRequest(name = "Basic", durationDays = 30, price = 9900)
        every { repo.save(any()) } answers { firstArg<Plan>().apply { id = 1L } }
        every { mapper.toDto(1L, any()) } returns mockk()

        service.create(request)

        io.mockk.verify { repo.save(match<Plan> { it.name == "Basic" && it.durationDays == 30 && it.price == 9900L }) }
    }

    // -- update --

    @Test
    fun `update modifies name and price`() {
        val plan = aPlan()
        every { repo.findById(1L) } returns Optional.of(plan)
        every { repo.save(plan) } returns plan
        every { mapper.toDto(1L, plan) } returns mockk()

        service.update(1L, UpdatePlanRequest(name = "Premium", price = 19900))

        assertEquals("Premium", plan.name)
        assertEquals(19900L, plan.price)
    }

    @Test
    fun `update throws EntityNotFoundException when plan not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.update(999L, UpdatePlanRequest(name = "x"))
        }
    }

    private fun aPlan() = Plan(
        name = "Basic",
        description = "Basic plan",
        durationDays = 30,
        price = 9900,
        enabled = true,
    ).apply { id = 1L }
}
