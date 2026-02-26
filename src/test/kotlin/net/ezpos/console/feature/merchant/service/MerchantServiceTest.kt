package net.ezpos.console.feature.merchant.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.merchant.dto.CreateMerchantRequest
import net.ezpos.console.feature.merchant.dto.MerchantDto
import net.ezpos.console.feature.merchant.dto.UpdateMerchantRequest
import net.ezpos.console.feature.merchant.entity.Merchant
import net.ezpos.console.feature.merchant.mapper.MerchantMapper
import net.ezpos.console.feature.merchant.repository.MerchantRepository
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MerchantServiceTest {

    private val repo = mockk<MerchantRepository>(relaxed = true)
    private val mapper = mockk<MerchantMapper>()
    private val service = MerchantService(repo, mapper)

    // ── create ──

    @Test
    fun `create saves merchant and returns dto`() {
        val merchant = aMerchant()
        every { repo.save(any()) } returns merchant
        every { mapper.toDto(1L, merchant) } returns aMerchantDto()

        val result = service.create(CreateMerchantRequest(name = "Test Shop"))

        assertEquals("Test Shop", result.name)
        verify { repo.save(any()) }
    }

    // ── getById ──

    @Test
    fun `getById throws EntityNotFoundException when merchant not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.getById(999L)
        }
    }

    @Test
    fun `getById returns dto when merchant found`() {
        val merchant = aMerchant()
        every { repo.findById(1L) } returns Optional.of(merchant)
        every { mapper.toDto(1L, merchant) } returns aMerchantDto()

        val result = service.getById(1L)

        assertEquals(1L, result.id)
        assertEquals("Test Shop", result.name)
    }

    // ── update ──

    @Test
    fun `update modifies fields`() {
        val merchant = aMerchant()
        every { repo.findById(1L) } returns Optional.of(merchant)
        every { repo.save(merchant) } returns merchant
        every { mapper.toDto(1L, merchant) } returns mockk()

        service.update(1L, UpdateMerchantRequest(name = "New Name", contactName = "John"))

        assertEquals("New Name", merchant.name)
        assertEquals("John", merchant.contactName)
    }

    @Test
    fun `update throws EntityNotFoundException when merchant not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.update(999L, UpdateMerchantRequest(name = "x"))
        }
    }

    // ── enable ──

    @Test
    fun `enable sets enabled to true`() {
        val merchant = aMerchant().apply { enabled = false }
        every { repo.findById(1L) } returns Optional.of(merchant)
        every { repo.save(merchant) } returns merchant
        every { mapper.toDto(1L, merchant) } returns mockk()

        service.enable(1L)

        assertTrue(merchant.enabled)
        verify { repo.save(merchant) }
    }

    @Test
    fun `enable throws EntityNotFoundException when merchant not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.enable(999L)
        }
    }

    // ── disable ──

    @Test
    fun `disable sets enabled to false`() {
        val merchant = aMerchant()
        every { repo.findById(1L) } returns Optional.of(merchant)
        every { repo.save(merchant) } returns merchant
        every { mapper.toDto(1L, merchant) } returns mockk()

        service.disable(1L)

        assertFalse(merchant.enabled)
        verify { repo.save(merchant) }
    }

    @Test
    fun `disable throws EntityNotFoundException when merchant not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.disable(999L)
        }
    }

    private fun aMerchant() = Merchant(
        name = "Test Shop",
        contactName = "Alice",
        contactPhone = "1234567890",
        address = "123 Main St",
        memo = "Test memo",
        enabled = true,
    ).apply { id = 1L }

    private fun aMerchantDto() = MerchantDto(
        id = 1L,
        name = "Test Shop",
        contactName = "Alice",
        contactPhone = "1234567890",
        address = "123 Main St",
        memo = "Test memo",
        enabled = true,
        createdAt = java.time.OffsetDateTime.now(),
        updatedAt = java.time.OffsetDateTime.now(),
    )
}
