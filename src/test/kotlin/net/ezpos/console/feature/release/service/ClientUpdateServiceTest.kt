package net.ezpos.console.feature.release.service

import io.mockk.every
import io.mockk.mockk
import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.DataIntegrityException
import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.entity.ReleaseApplication
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import net.ezpos.console.feature.release.model.ReleaseStatus
import net.ezpos.console.feature.release.repository.ReleaseRepository
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientUpdateServiceTest {

    private val appService = mockk<ReleaseApplicationService>()
    private val releaseRepo = mockk<ReleaseRepository>()
    private val service = ClientUpdateService(appService, releaseRepo)

    private fun enabledApp(code: String = "app1"): ReleaseApplication =
        ReleaseApplication(code = code, name = "Test App").apply { enabled = true }

    /** 通过反射设置 IdEntity 的只读 id 字段（测试专用）。 */
    private fun setEntityId(entity: Any, id: Long) {
        val field = entity.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }

    private fun publishedRelease(
        version: String = "2.0.0",
        minSupportedVersion: String = "1.0.0",
        artifactUrl: String = "https://dl.example.com/v2.zip",
    ): Release = Release(
        applicationCode = "app1",
        platform = "android",
        version = version,
        minSupportedVersion = minSupportedVersion,
    ).apply {
        setEntityId(this, 100L)
        this.status = ReleaseStatus.PUBLISHED
        this.artifactUrl = artifactUrl
        this.rolloutType = ReleaseRolloutType.ALL
    }

    @Test
    fun `returns no update when no published release exists`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns null

        val result = service.check("app1", "android", "1.0.0", "tenant-1", null)
        assertFalse(result.updateAvailable)
    }

    @Test
    fun `returns no update when current version equals latest`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(version = "1.0.0")

        val result = service.check("app1", "android", "1.0.0", "tenant-1", null)
        assertFalse(result.updateAvailable)
    }

    @Test
    fun `returns no update when current version is newer`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(version = "1.0.0")

        val result = service.check("app1", "android", "2.0.0", "tenant-1", null)
        assertFalse(result.updateAvailable)
    }

    @Test
    fun `returns forced update when current version below minSupportedVersion`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(version = "3.0.0", minSupportedVersion = "2.0.0")

        val result = service.check("app1", "android", "1.5.0", "tenant-1", null)
        assertTrue(result.updateAvailable)
        assertTrue(result.isForced == true)
    }

    @Test
    fun `returns normal update when newer version available`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(version = "2.0.0")

        val result = service.check("app1", "android", "1.0.0", "tenant-1", null)
        assertTrue(result.updateAvailable)
        assertEquals("2.0.0", result.latestVersion)
    }

    @Test
    fun `throws BusinessRuleException for invalid current version`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease()

        assertThrows<BusinessRuleException> {
            service.check("app1", "android", "not-a-version", "tenant-1", null)
        }
    }

    @Test
    fun `throws DataIntegrityException when stored version is corrupt`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(version = "corrupt")

        assertThrows<DataIntegrityException> {
            service.check("app1", "android", "1.0.0", "tenant-1", null)
        }
    }

    @Test
    fun `throws DataIntegrityException when artifactUrl is not set`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatformAndStatus("app1", "android", ReleaseStatus.PUBLISHED) } returns
            publishedRelease(artifactUrl = "")

        assertThrows<DataIntegrityException> {
            service.check("app1", "android", "1.0.0", "tenant-1", null)
        }
    }
}
