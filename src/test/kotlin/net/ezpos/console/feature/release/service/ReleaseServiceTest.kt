package net.ezpos.console.feature.release.service

import io.mockk.every
import io.mockk.mockk
import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.release.dto.CreateReleaseRequest
import net.ezpos.console.feature.release.dto.ReleaseDto
import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.entity.ReleaseApplication
import net.ezpos.console.feature.release.mapper.ReleaseMapper
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import net.ezpos.console.feature.release.model.ReleaseStatus
import net.ezpos.console.feature.release.repository.ReleaseRepository
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseServiceTest {

    private val releaseRepo = mockk<ReleaseRepository>()
    private val appService = mockk<ReleaseApplicationService>()
    private val mapper = mockk<ReleaseMapper>()
    private val service = ReleaseService(releaseRepo, appService, mapper)

    private val dummyDto = mockk<ReleaseDto>(relaxed = true)

    private fun aRelease(
        version: String = "1.0.0",
        minSupportedVersion: String = "1.0.0",
        status: ReleaseStatus = ReleaseStatus.PAUSED,
        artifactUrl: String? = "https://example.com/artifact.zip",
        artifactKey: String? = null,
    ): Release = Release(
        applicationCode = "test-app",
        platform = "android",
        version = version,
        minSupportedVersion = minSupportedVersion,
    ).apply {
        this.status = status
        this.artifactUrl = artifactUrl
        this.artifactKey = artifactKey
        this.rolloutType = ReleaseRolloutType.ALL
    }

    private fun enabledApp(): ReleaseApplication =
        ReleaseApplication(code = "test-app", name = "Test App").apply { enabled = true }

    // ── getById ──

    @Test
    fun `getById throws EntityNotFoundException when release does not exist`() {
        every { releaseRepo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> { service.getById(999L) }
    }

    @Test
    fun `getById returns dto when release exists`() {
        val release = aRelease()
        every { releaseRepo.findById(1L) } returns Optional.of(release)
        every { mapper.toDto(release) } returns dummyDto

        val result = service.getById(1L)
        assertEquals(dummyDto, result)
    }

    // ── create: 版本号校验 ──

    @Test
    fun `create throws BusinessRuleException for invalid version format`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "not-a-version",
            minSupportedVersion = "1.0.0",
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    @Test
    fun `create throws BusinessRuleException for invalid minSupportedVersion`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "bad",
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    @Test
    fun `create throws BusinessRuleException when minSupportedVersion greater than version`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "2.0.0",
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    // ── create: 唯一约束 ──

    @Test
    fun `create throws EntityAlreadyExistsException when release exists for app+platform`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns aRelease()

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "1.0.0",
        )
        assertThrows<EntityAlreadyExistsException> { service.create(req) }
    }

    // ── create: 灰度校验 ──

    @Test
    fun `create throws BusinessRuleException when percent rollout without percent value`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "1.0.0",
            rolloutType = ReleaseRolloutType.PERCENT,
            percent = null,
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    @Test
    fun `create throws BusinessRuleException when whitelist rollout with empty list`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "1.0.0",
            rolloutType = ReleaseRolloutType.WHITELIST,
            whitelistTenants = emptyList(),
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    @Test
    fun `create throws BusinessRuleException when percent out of range`() {
        every { appService.requireEnabled("app1") } returns enabledApp()
        every { releaseRepo.findByApplicationCodeAndPlatform("app1", "android") } returns null

        val req = CreateReleaseRequest(
            applicationCode = "app1",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "1.0.0",
            rolloutType = ReleaseRolloutType.PERCENT,
            percent = 150,
        )
        assertThrows<BusinessRuleException> { service.create(req) }
    }

    // ── publish ──

    @Test
    fun `publish throws EntityNotFoundException when release not found`() {
        every { releaseRepo.findById(1L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> { service.publish(1L) }
    }

    @Test
    fun `publish throws BusinessRuleException when no artifact info`() {
        val release = aRelease(artifactUrl = null, artifactKey = null)
        every { releaseRepo.findById(1L) } returns Optional.of(release)
        assertThrows<BusinessRuleException> { service.publish(1L) }
    }

    @Test
    fun `publish sets status to PUBLISHED`() {
        val release = aRelease(artifactUrl = "https://dl.example.com/v1.zip")
        every { releaseRepo.findById(1L) } returns Optional.of(release)
        every { releaseRepo.save(any()) } answers { firstArg() }
        every { mapper.toDto(any<Release>()) } returns dummyDto

        service.publish(1L)
        assertEquals(ReleaseStatus.PUBLISHED, release.status)
    }

    // ── pause ──

    @Test
    fun `pause sets status to PAUSED`() {
        val release = aRelease(status = ReleaseStatus.PUBLISHED)
        every { releaseRepo.findById(1L) } returns Optional.of(release)
        every { releaseRepo.save(any()) } answers { firstArg() }
        every { mapper.toDto(any<Release>()) } returns dummyDto

        service.pause(1L)
        assertEquals(ReleaseStatus.PAUSED, release.status)
    }

    // ── resume ──

    @Test
    fun `resume throws BusinessRuleException when no artifact`() {
        val release = aRelease(artifactUrl = null, artifactKey = null, status = ReleaseStatus.PAUSED)
        every { releaseRepo.findById(1L) } returns Optional.of(release)
        assertThrows<BusinessRuleException> { service.resume(1L) }
    }
}
