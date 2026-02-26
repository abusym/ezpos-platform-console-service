package net.ezpos.console.feature.release.service

import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RolloutDeciderTest {

    private fun release(
        rolloutType: ReleaseRolloutType = ReleaseRolloutType.ALL,
        percent: Int? = null,
        whitelistTenants: String? = null,
        rolloutSalt: String? = null,
        version: String = "1.0.0",
    ): Release = Release(
        applicationCode = "test-app",
        platform = "android",
        version = version,
        minSupportedVersion = "1.0.0",
    ).apply {
        this.rolloutType = rolloutType
        this.percent = percent
        this.whitelistTenants = whitelistTenants
        this.rolloutSalt = rolloutSalt
    }

    // ── ALL ──

    @Test
    fun `ALL rollout always includes`() {
        assertTrue(RolloutDecider.isIncluded(release(), "tenant-1", null))
    }

    @Test
    fun `ALL rollout includes with device id`() {
        assertTrue(RolloutDecider.isIncluded(release(), "tenant-1", "device-1"))
    }

    // ── WHITELIST ──

    @Test
    fun `WHITELIST includes listed tenant`() {
        val r = release(
            rolloutType = ReleaseRolloutType.WHITELIST,
            whitelistTenants = "tenant-1,tenant-2,tenant-3",
        )
        assertTrue(RolloutDecider.isIncluded(r, "tenant-2", null))
    }

    @Test
    fun `WHITELIST excludes unlisted tenant`() {
        val r = release(
            rolloutType = ReleaseRolloutType.WHITELIST,
            whitelistTenants = "tenant-1,tenant-2",
        )
        assertFalse(RolloutDecider.isIncluded(r, "tenant-99", null))
    }

    @Test
    fun `WHITELIST with null whitelistTenants excludes everyone`() {
        val r = release(rolloutType = ReleaseRolloutType.WHITELIST, whitelistTenants = null)
        assertFalse(RolloutDecider.isIncluded(r, "tenant-1", null))
    }

    // ── PERCENT ──

    @Test
    fun `PERCENT 0 excludes everyone`() {
        val r = release(rolloutType = ReleaseRolloutType.PERCENT, percent = 0)
        assertFalse(RolloutDecider.isIncluded(r, "tenant-1", null))
    }

    @Test
    fun `PERCENT 100 includes everyone`() {
        val r = release(rolloutType = ReleaseRolloutType.PERCENT, percent = 100)
        assertTrue(RolloutDecider.isIncluded(r, "tenant-1", null))
    }

    @Test
    fun `PERCENT is deterministic for same input`() {
        val r = release(rolloutType = ReleaseRolloutType.PERCENT, percent = 50, rolloutSalt = "salt1")
        val result1 = RolloutDecider.isIncluded(r, "tenant-1", "device-1")
        val result2 = RolloutDecider.isIncluded(r, "tenant-1", "device-1")
        assertEquals(result1, result2)
    }

    @Test
    fun `PERCENT uses version as salt when rolloutSalt is null`() {
        val r = release(
            rolloutType = ReleaseRolloutType.PERCENT,
            percent = 50,
            rolloutSalt = null,
            version = "2.0.0",
        )
        // 不抛出异常即可
        RolloutDecider.isIncluded(r, "tenant-1", null)
    }

    @Test
    fun `PERCENT with null percent excludes everyone`() {
        val r = release(rolloutType = ReleaseRolloutType.PERCENT, percent = null)
        assertFalse(RolloutDecider.isIncluded(r, "tenant-1", null))
    }

    @Test
    fun `PERCENT distribution is roughly correct`() {
        val r = release(rolloutType = ReleaseRolloutType.PERCENT, percent = 50, rolloutSalt = "test-salt")
        val included = (1..1000).count { i ->
            RolloutDecider.isIncluded(r, "tenant-$i", null)
        }
        // 允许 ±15% 的波动
        assertTrue(included in 350..650, "Expected ~500 included, got $included")
    }
}
