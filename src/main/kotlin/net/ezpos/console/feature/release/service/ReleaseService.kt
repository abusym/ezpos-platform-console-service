package net.ezpos.console.feature.release.service

import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.DataIntegrityException
import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.release.dto.CompleteArtifactRequest
import net.ezpos.console.feature.release.dto.CreateReleaseRequest
import net.ezpos.console.feature.release.dto.ReleaseDto
import net.ezpos.console.feature.release.dto.UpdateReleaseRequest
import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.mapper.ReleaseMapper
import net.ezpos.console.feature.release.model.ReleaseRolloutType
import net.ezpos.console.feature.release.model.ReleaseStatus
import net.ezpos.console.feature.release.model.SemVer
import net.ezpos.console.feature.release.repository.ReleaseRepository
import net.ezpos.console.feature.release.repository.ReleaseSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * 发布配置（[Release]）管理服务。
 *
 * 覆盖控制台侧的主要用例：
 * - 查询发布配置列表/详情
 * - 创建/局部更新发布配置，并对版本号与灰度字段组合做校验
 * - 发布/暂停/恢复发布（状态流转）及发布前置条件校验
 * - 写入制品信息（artifact 完成）
 *
 * 注意：部分列表过滤在内存中进行（先 `findAll` 再 filter），适用于数据量较小的管理场景。
 */
@Service
class ReleaseService(
    private val releaseRepo: ReleaseRepository,
    private val appService: ReleaseApplicationService,
    private val mapper: ReleaseMapper,
) {
    /**
     * 列表查询，可按应用编码/平台/状态过滤。
     *
     * @param applicationCode 可选：应用编码（忽略空白）
     * @param platform 可选：平台（忽略空白）
     * @param status 可选：状态
     */
    fun list(
        applicationCode: String?,
        platform: String?,
        status: ReleaseStatus?,
        pageable: Pageable,
    ): Page<ReleaseDto> {
        val defaultSort = Sort.by(
            Sort.Order.desc("updatedAt").nullsLast(),
            Sort.Order.asc("applicationCode"),
            Sort.Order.asc("platform"),
        )
        val effectivePageable =
            if (pageable.sort.isSorted) pageable else PageRequest.of(pageable.pageNumber, pageable.pageSize, defaultSort)

        val spec = ReleaseSpecifications.filter(applicationCode, platform, status)
        return releaseRepo.findAll(spec, effectivePageable).map { mapper.toDto(it) }
    }

    /**
     * 按 id 获取发布配置详情。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     */
    fun getById(id: Long): ReleaseDto =
        mapper.toDto(findOrThrow(id))

    /**
     * 创建某应用/平台的发布配置（create-only）。
     *
     * 关键校验：
     * - 应用必须存在且启用
     * - `version` 与 `minSupportedVersion` 必须为 `x.y.z` 且满足 `minSupportedVersion <= version`
     * - 灰度字段组合必须与 [CreateReleaseRequest.rolloutType] 匹配
     *
     * 约定：新建记录默认置为 [ReleaseStatus.PAUSED]，需要显式发布后才对客户端生效。
     *
     * @throws EntityAlreadyExistsException 当同一 applicationCode + platform 的记录已存在时抛出
     */
    @Transactional
    fun create(request: CreateReleaseRequest): ReleaseDto {
        val applicationCode = request.applicationCode.trim()
        val platform = request.platform.trim()

        appService.requireEnabled(applicationCode)

        val existing = releaseRepo.findByApplicationCodeAndPlatform(applicationCode, platform)
        if (existing != null) {
            throw EntityAlreadyExistsException(
                "Release already exists for applicationCode=$applicationCode platform=$platform",
            )
        }

        val version = request.version.trim()
        val minSupported = request.minSupportedVersion.trim()
        val v = SemVer.parse(version) ?: throw BusinessRuleException("Invalid version: $version")
        val minV = SemVer.parse(minSupported)
            ?: throw BusinessRuleException("Invalid minSupportedVersion: $minSupported")
        if (minV > v) {
            throw BusinessRuleException("minSupportedVersion must be <= version")
        }

        validateRollout(request.rolloutType, request.percent, request.whitelistTenants)

        val release = Release(
            applicationCode = applicationCode,
            platform = platform,
            version = version,
            minSupportedVersion = minSupported,
        ).also { it.status = ReleaseStatus.PAUSED }

        applyCreate(release, request)

        val saved = releaseRepo.save(release)
        return mapper.toDto(saved)
    }

    /**
     * 按 id 局部更新发布配置（Patch/Update 语义）。
     *
     * `null` 字段表示不修改；非 `null` 字段会写入并做必要的联动校验（例如版本/最低支持版本关系、灰度字段组合）。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     * @throws BusinessRuleException 当输入不合法时抛出
     */
    @Transactional
    fun update(id: Long, request: UpdateReleaseRequest): ReleaseDto {
        val release = findOrThrow(id)

        if (request.version != null) {
            val v = SemVer.parse(request.version.trim())
                ?: throw BusinessRuleException("Invalid version: ${request.version}")
            release.version = v.toString()
        }
        if (request.minSupportedVersion != null) {
            val minV = SemVer.parse(request.minSupportedVersion.trim())
                ?: throw BusinessRuleException("Invalid minSupportedVersion: ${request.minSupportedVersion}")
            val currentV = SemVer.parse(release.version)
                ?: throw DataIntegrityException("Invalid stored version in release $id")
            if (minV > currentV) {
                throw BusinessRuleException("minSupportedVersion must be <= version")
            }
            release.minSupportedVersion = minV.toString()
        }

        request.releaseNotes?.let { release.releaseNotes = it.trim().ifEmpty { null } }
        request.artifactKey?.let { release.artifactKey = it.trim().ifEmpty { null } }
        request.artifactUrl?.let { release.artifactUrl = it.trim().ifEmpty { null } }
        request.sha256?.let { release.sha256 = it.trim().ifEmpty { null } }
        request.fileSize?.let { release.fileSize = it }
        request.isForced?.let { release.isForced = it }
        if (request.forceAfterAt != null) {
            release.forceAfterAt = request.forceAfterAt
        }
        request.rolloutSalt?.let { release.rolloutSalt = it.trim().ifEmpty { null } }

        val effectiveRolloutType = request.rolloutType ?: release.rolloutType
        val effectivePercent = request.percent ?: release.percent
        val effectiveWhitelist = request.whitelistTenants ?: WhitelistTenantsCodec.decode(release.whitelistTenants)
        validateRollout(effectiveRolloutType, effectivePercent, effectiveWhitelist)

        release.rolloutType = effectiveRolloutType
        when (effectiveRolloutType) {
            ReleaseRolloutType.ALL -> {
                release.percent = null
                release.whitelistTenants = null
            }
            ReleaseRolloutType.PERCENT -> {
                release.percent = effectivePercent
                release.whitelistTenants = null
            }
            ReleaseRolloutType.WHITELIST -> {
                release.percent = null
                release.whitelistTenants = WhitelistTenantsCodec.encode(effectiveWhitelist)
            }
        }

        return mapper.toDto(releaseRepo.save(release))
    }

    /**
     * 将发布配置切换为"发布"状态。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     * @throws BusinessRuleException 当发布前置条件不满足时抛出（例如制品信息缺失、版本号不合法等）
     */
    @Transactional
    fun publish(id: Long): ReleaseDto {
        val release = findOrThrow(id)
        validatePublishable(release)
        if (release.publishedAt == null) {
            release.publishedAt = OffsetDateTime.now()
        }
        release.status = ReleaseStatus.PUBLISHED
        return mapper.toDto(releaseRepo.save(release))
    }

    /**
     * 将发布配置切换为"暂停"状态（暂停对客户端生效）。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     */
    @Transactional
    fun pause(id: Long): ReleaseDto {
        val release = findOrThrow(id)
        release.status = ReleaseStatus.PAUSED
        return mapper.toDto(releaseRepo.save(release))
    }

    /**
     * 从暂停状态恢复发布（再次对客户端生效）。
     *
     * 与 [publish] 类似，会校验发布前置条件；若首次恢复且 [Release.publishedAt] 为空，则填充当前时间。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     * @throws BusinessRuleException 当发布前置条件不满足时抛出
     */
    @Transactional
    fun resume(id: Long): ReleaseDto {
        val release = findOrThrow(id)
        validatePublishable(release)
        if (release.publishedAt == null) {
            release.publishedAt = OffsetDateTime.now()
        }
        release.status = ReleaseStatus.PUBLISHED
        return mapper.toDto(releaseRepo.save(release))
    }

    /**
     * 写入/更新制品信息（例如上传完成后的 URL、校验和、大小）。
     *
     * @throws EntityNotFoundException 当记录不存在时抛出
     */
    @Transactional
    fun completeArtifact(id: Long, request: CompleteArtifactRequest): ReleaseDto {
        val release = findOrThrow(id)
        if (request.artifactKey != null) release.artifactKey = request.artifactKey.trim().ifEmpty { null }
        if (request.artifactUrl != null) release.artifactUrl = request.artifactUrl.trim().ifEmpty { null }
        if (request.sha256 != null) release.sha256 = request.sha256.trim().ifEmpty { null }
        if (request.fileSize != null) release.fileSize = request.fileSize
        return mapper.toDto(releaseRepo.save(release))
    }

    private fun findOrThrow(id: Long): Release =
        releaseRepo.findById(id).orElseThrow { EntityNotFoundException("Release", id) }

    /**
     * 将创建请求体的字段应用到实体。
     *
     * 该方法假设调用者已完成版本号与灰度字段组合的校验。
     */
    private fun applyCreate(release: Release, request: CreateReleaseRequest) {
        release.version = request.version.trim()
        release.minSupportedVersion = request.minSupportedVersion.trim()
        release.releaseNotes = request.releaseNotes?.trim()
        release.artifactKey = request.artifactKey?.trim()
        release.artifactUrl = request.artifactUrl?.trim()
        release.sha256 = request.sha256?.trim()
        release.fileSize = request.fileSize
        release.isForced = request.isForced
        release.forceAfterAt = request.forceAfterAt
        release.rolloutType = request.rolloutType
        release.rolloutSalt = request.rolloutSalt?.trim()?.ifEmpty { null }

        when (request.rolloutType) {
            ReleaseRolloutType.ALL -> {
                release.percent = null
                release.whitelistTenants = null
            }
            ReleaseRolloutType.PERCENT -> {
                release.percent = request.percent
                release.whitelistTenants = null
            }
            ReleaseRolloutType.WHITELIST -> {
                release.percent = null
                release.whitelistTenants = WhitelistTenantsCodec.encode(request.whitelistTenants)
            }
        }
    }

    /**
     * 校验发布配置是否满足"可发布"的前置条件。
     *
     * 典型约束：
     * - 必须具备可下载的制品信息（artifactUrl 或 artifactKey）
     * - 版本号必须可解析且满足 `minSupportedVersion <= version`
     * - 灰度字段组合必须正确
     */
    private fun validatePublishable(release: Release) {
        if (release.artifactUrl.isNullOrBlank() && release.artifactKey.isNullOrBlank()) {
            throw BusinessRuleException("artifactUrl or artifactKey is required before publish")
        }
        val v = SemVer.parse(release.version) ?: throw BusinessRuleException("Invalid version")
        val minV = SemVer.parse(release.minSupportedVersion)
            ?: throw BusinessRuleException("Invalid minSupportedVersion")
        if (minV > v) {
            throw BusinessRuleException("minSupportedVersion must be <= version")
        }
        validateRollout(release.rolloutType, release.percent, WhitelistTenantsCodec.decode(release.whitelistTenants))
    }

    /**
     * 校验灰度字段组合是否合法。
     *
     * @throws BusinessRuleException 当组合不合法时抛出
     */
    private fun validateRollout(
        rolloutType: ReleaseRolloutType,
        percent: Int?,
        whitelistTenants: List<String>?,
    ) {
        when (rolloutType) {
            ReleaseRolloutType.ALL -> Unit
            ReleaseRolloutType.PERCENT -> {
                val p = percent ?: throw BusinessRuleException("percent is required for rolloutType=percent")
                if (p !in 0..100) throw BusinessRuleException("percent must be between 0 and 100")
            }
            ReleaseRolloutType.WHITELIST -> {
                val list = whitelistTenants ?: emptyList()
                if (list.isEmpty()) {
                    throw BusinessRuleException("whitelistTenants is required for rolloutType=whitelist")
                }
            }
        }
    }
}
