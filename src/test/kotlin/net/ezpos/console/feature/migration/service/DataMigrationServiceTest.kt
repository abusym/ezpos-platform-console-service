package net.ezpos.console.feature.migration.service

import io.mockk.every
import io.mockk.mockk
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.migration.dto.CreateDataMigrationRequest
import net.ezpos.console.feature.migration.dto.DataMigrationDto
import net.ezpos.console.feature.migration.entity.DataMigration
import net.ezpos.console.feature.migration.mapper.DataMigrationMapper
import net.ezpos.console.feature.migration.model.MigrationStatus
import net.ezpos.console.feature.migration.model.MigrationType
import net.ezpos.console.feature.migration.repository.DataMigrationRepository
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class DataMigrationServiceTest {

    private val repo = mockk<DataMigrationRepository>(relaxed = true)
    private val mapper = mockk<DataMigrationMapper>()
    private val service = DataMigrationService(repo, mapper)

    // -- create --

    @Test
    fun `create sets status PENDING and progress 0`() {
        val request = CreateDataMigrationRequest(
            title = "Migrate products",
            description = "Full product migration",
            type = "product",
        )
        val entity = aMigration()
        val dto = aDto()

        every { repo.save(any()) } returns entity
        every { mapper.toDto(1L, entity) } returns dto

        val result = service.create(request)

        assertEquals("pending", result.status)
        assertEquals(0, result.progress)
    }

    // -- getById --

    @Test
    fun `getById throws EntityNotFoundException when not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.getById(999L)
        }
    }

    @Test
    fun `getById returns dto when found`() {
        val entity = aMigration()
        val dto = aDto()

        every { repo.findById(1L) } returns Optional.of(entity)
        every { mapper.toDto(1L, entity) } returns dto

        val result = service.getById(1L)
        assertEquals(1L, result.id)
        assertEquals("Migrate products", result.title)
    }

    private fun aMigration() = DataMigration(
        title = "Migrate products",
        description = "Full product migration",
        type = MigrationType.PRODUCT,
        status = MigrationStatus.PENDING,
        progress = 0,
    ).apply { id = 1L }

    private fun aDto() = DataMigrationDto(
        id = 1L,
        title = "Migrate products",
        description = "Full product migration",
        type = "product",
        status = "pending",
        progress = 0,
    )
}
