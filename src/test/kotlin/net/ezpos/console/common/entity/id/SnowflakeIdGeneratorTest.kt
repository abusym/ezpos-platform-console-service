package net.ezpos.console.common.entity.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnowflakeIdGeneratorTest {

    private val epoch = 1735689600000L
    private val machineId = 1L

    @Test
    fun `generates positive IDs`() {
        val id = SnowflakeIdGenerator.nextId(epoch, machineId)
        assertTrue(id > 0, "ID should be positive but was $id")
    }

    @Test
    fun `generates unique IDs across sequential calls`() {
        val ids = (1..1000).map { SnowflakeIdGenerator.nextId(epoch, machineId) }.toSet()
        assertEquals(1000, ids.size, "Expected 1000 unique IDs")
    }

    @Test
    fun `IDs are monotonically non-decreasing`() {
        val ids = (1..100).map { SnowflakeIdGenerator.nextId(epoch, machineId) }
        for (i in 1 until ids.size) {
            assertTrue(
                ids[i] >= ids[i - 1],
                "ID at index $i (${ids[i]}) should be >= ID at index ${i - 1} (${ids[i - 1]})",
            )
        }
    }

    @Test
    fun `different machineIds produce different IDs`() {
        val id1 = SnowflakeIdGenerator.nextId(epoch, 1L)
        val id2 = SnowflakeIdGenerator.nextId(epoch, 2L)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `Hibernate generate returns Serializable`() {
        val generator = SnowflakeIdGenerator(machineId, epoch)
        val id = generator.generate(null, null)
        assertTrue(id is Long, "Generated ID should be Long")
        assertTrue((id as Long) > 0)
    }
}
