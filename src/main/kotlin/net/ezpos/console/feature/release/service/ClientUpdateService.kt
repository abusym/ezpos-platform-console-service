package net.ezpos.console.feature.release.service

import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.DataIntegrityException
import net.ezpos.console.feature.release.dto.ClientUpdateCheckResponse
import net.ezpos.console.feature.release.model.ReleaseStatus
import net.ezpos.console.feature.release.model.SemVer
import net.ezpos.console.feature.release.repository.ReleaseRepository
import org.springframework.stereotype.Service

/**
 * 客户端更新检查服务。
 *
 * 该服务面向"客户端自助检查更新"的调用场景，核心职责：
 * - 校验应用是否启用
 * - 获取当前平台处于发布状态的发布配置
 * - 比较客户端当前版本与最新版本/最低支持版本
 * - 按灰度规则（全量/百分比/白名单）决定当前租户/设备是否命中更新
 */
@Service
class ClientUpdateService(
    private val appService: ReleaseApplicationService,
    private val releaseRepo: ReleaseRepository,
) {
    /**
     * 检查是否存在对当前租户/设备可见的更新。
     *
     * 判定顺序（高优先级在前）：
     * - 应用未启用/不存在：抛出 [net.ezpos.console.common.exception.EntityNotFoundException]
     * - 无发布中的发布配置：返回无更新
     * - 当前版本不合法：抛出 [BusinessRuleException]
     * - 当前版本低于最低支持版本：返回强制更新（不受灰度规则影响）
     * - 当前版本已是最新或更高：返回无更新
     * - 未命中灰度规则：返回无更新
     * - 命中灰度规则：返回可更新信息（可选强制）
     *
     * @param appCode 应用编码
     * @param platform 平台标识
     * @param currentVersion 客户端当前版本号（要求为 `x.y.z` 三段式）
     * @param tenantId 租户 id（用于灰度命中判定）
     * @param deviceId 设备 id（可选；用于百分比灰度进一步打散）
     */
    fun check(
        appCode: String,
        platform: String,
        currentVersion: String,
        tenantId: String,
        deviceId: String?,
    ): ClientUpdateCheckResponse {
        val app = appService.requireEnabled(appCode)

        val release = releaseRepo.findByApplicationCodeAndPlatformAndStatus(app.code, platform.trim(), ReleaseStatus.PUBLISHED)
            ?: return ClientUpdateCheckResponse(updateAvailable = false)

        val current = SemVer.parse(currentVersion)
            ?: throw BusinessRuleException("Invalid current version: $currentVersion")
        val latest = SemVer.parse(release.version)
            ?: throw DataIntegrityException("Invalid release.version")
        val minSupported = SemVer.parse(release.minSupportedVersion)
            ?: throw DataIntegrityException("Invalid release.minSupportedVersion")

        val url = release.artifactUrl?.takeIf { it.isNotBlank() }
            ?: throw DataIntegrityException("Release artifactUrl is not set")

        val forcedByMinSupported = current < minSupported
        if (forcedByMinSupported) {
            return ClientUpdateCheckResponse(
                updateAvailable = true,
                releaseId = requireNotNull(release.id),
                latestVersion = latest.toString(),
                minSupportedVersion = minSupported.toString(),
                isForced = true,
                forceAfterAt = release.forceAfterAt,
                releaseNotes = release.releaseNotes,
                download = ClientUpdateCheckResponse.DownloadInfo(
                    url = url,
                    sha256 = release.sha256,
                    fileSize = release.fileSize,
                ),
            )
        }

        if (latest <= current) {
            return ClientUpdateCheckResponse(updateAvailable = false)
        }

        val included = RolloutDecider.isIncluded(release, tenantId, deviceId)
        if (!included) {
            return ClientUpdateCheckResponse(updateAvailable = false)
        }

        return ClientUpdateCheckResponse(
            updateAvailable = true,
            releaseId = requireNotNull(release.id),
            latestVersion = latest.toString(),
            minSupportedVersion = minSupported.toString(),
            isForced = release.isForced,
            forceAfterAt = release.forceAfterAt,
            releaseNotes = release.releaseNotes,
            download = ClientUpdateCheckResponse.DownloadInfo(
                url = url,
                sha256 = release.sha256,
                fileSize = release.fileSize,
            ),
        )
    }
}
