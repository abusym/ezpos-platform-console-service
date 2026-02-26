package net.ezpos.console.feature.release.service

import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.release.dto.CreateReleaseApplicationRequest
import net.ezpos.console.feature.release.dto.ReleaseApplicationDto
import net.ezpos.console.feature.release.dto.UpdateReleaseApplicationRequest
import net.ezpos.console.feature.release.entity.ReleaseApplication
import net.ezpos.console.feature.release.mapper.ReleaseApplicationMapper
import net.ezpos.console.feature.release.repository.ReleaseApplicationRepository
import org.springframework.stereotype.Service

/**
 * 可发布应用（Release Application）相关的业务服务。
 *
 * 负责：
 * - 应用主数据的增删改查（本实现提供 list/get/create/update）
 * - 在发布相关流程中校验应用是否存在且处于启用状态（[requireEnabled]）
 */
@Service
class ReleaseApplicationService(
    private val repo: ReleaseApplicationRepository,
    private val mapper: ReleaseApplicationMapper,
) {
    /**
     * 列出所有可发布应用，按 `code` 排序。
     */
    fun list(): List<ReleaseApplicationDto> =
        repo.findAll()
            .sortedBy { it.code }
            .map { toDto(it) }

    /**
     * 按应用编码查询。
     *
     * @throws EntityNotFoundException 当应用不存在时抛出
     */
    fun getByCode(code: String): ReleaseApplicationDto =
        repo.findByCode(code.trim())
            ?.let { toDto(it) }
            ?: throw EntityNotFoundException("Application", code)

    /**
     * 创建新的可发布应用。
     *
     * @throws EntityAlreadyExistsException 当 `code` 已存在时抛出
     */
    fun create(request: CreateReleaseApplicationRequest): ReleaseApplicationDto {
        val code = request.code.trim()
        if (repo.existsByCode(code)) {
            throw EntityAlreadyExistsException("Application code already exists: $code")
        }
        val app = ReleaseApplication(
            code = code,
            name = request.name.trim(),
            description = request.description?.trim(),
            enabled = request.enabled,
        )
        return toDto(repo.save(app))
    }

    /**
     * 按应用编码局部更新应用信息。
     *
     * @throws EntityNotFoundException 当应用不存在时抛出
     */
    fun update(code: String, request: UpdateReleaseApplicationRequest): ReleaseApplicationDto {
        val app = repo.findByCode(code.trim())
            ?: throw EntityNotFoundException("Application", code)

        request.name?.let { app.name = it.trim() }
        if (request.description != null) {
            app.description = request.description.trim().ifEmpty { null }
        }
        request.enabled?.let { app.enabled = it }

        return toDto(repo.save(app))
    }

    /**
     * 获取指定应用，并要求其处于启用状态。
     *
     * 该方法常用于发布/客户端检查更新等流程中，确保对外生效的发布配置一定归属于启用应用。
     *
     * @throws EntityNotFoundException 当应用不存在或已禁用时抛出
     */
    fun requireEnabled(code: String): ReleaseApplication =
        repo.findByCode(code.trim())
            ?.takeIf { it.enabled }
            ?: throw EntityNotFoundException("Application", code)

    private fun toDto(app: ReleaseApplication): ReleaseApplicationDto =
        mapper.toDto(requireNotNull(app.id), app)
}
