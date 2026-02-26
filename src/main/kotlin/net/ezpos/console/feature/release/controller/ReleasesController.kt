package net.ezpos.console.feature.release.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import net.ezpos.console.feature.release.dto.ArtifactPresignResponse
import net.ezpos.console.feature.release.dto.CompleteArtifactRequest
import net.ezpos.console.feature.release.dto.CreateReleaseRequest
import net.ezpos.console.feature.release.dto.ReleaseDto
import net.ezpos.console.feature.release.dto.UpdateReleaseRequest
import net.ezpos.console.feature.release.model.ReleaseStatus
import net.ezpos.console.feature.release.service.ReleaseService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * 发布配置管理接口（控制台侧）。
 *
 * Base path: `/api/releases`
 *
 * 该控制器主要提供发布配置的 CRUD、状态流转（发布/暂停/恢复）以及制品信息补全等能力。
 * 入参校验主要依赖 Jakarta Validation（`@Valid` + DTO 上的约束注解），业务校验由 [ReleaseService] 完成。
 */
@Tag(name = "版本发布", description = "发布配置的增删改查、状态流转与制品管理")
@RestController
@RequestMapping("/api/releases")
class ReleasesController(
    private val service: ReleaseService,
) {
    /**
     * 列表查询发布配置。
     *
     * @param applicationCode 可选：按应用编码过滤
     * @param platform 可选：按平台过滤
     * @param status 可选：按状态过滤
     */
    @Operation(summary = "分页查询发布配置")
    @GetMapping
    fun list(
        @Parameter(description = "应用编码") @RequestParam(required = false) applicationCode: String?,
        @Parameter(description = "平台标识") @RequestParam(required = false) platform: String?,
        @Parameter(description = "发布状态") @RequestParam(required = false) status: ReleaseStatus?,
        pageable: Pageable,
    ): Page<ReleaseDto> = service.list(applicationCode, platform, status, pageable)

    /**
     * 获取发布配置详情。
     *
     * @throws ResponseStatusException 404 当记录不存在时抛出
     */
    @Operation(summary = "获取发布配置详情")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ReleaseDto = service.getById(id)

    /**
     * 创建发布配置（create-only）。
     */
    @Operation(summary = "创建发布配置")
    @PostMapping
    fun create(@Valid @RequestBody request: CreateReleaseRequest): ReleaseDto = service.create(request)

    /**
     * 局部更新发布配置（Patch/Update 语义）。
     */
    @Operation(summary = "更新发布配置")
    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateReleaseRequest): ReleaseDto =
        service.update(id, request)

    /**
     * 发布：将发布配置切换为发布状态，使其可被客户端命中（仍受灰度规则影响）。
     */
    @Operation(summary = "发布")
    @PostMapping("/{id}:publish")
    fun publish(@PathVariable id: Long): ReleaseDto = service.publish(id)

    /**
     * 暂停：将发布配置切换为暂停状态，使其不再对客户端生效。
     */
    @Operation(summary = "暂停发布")
    @PostMapping("/{id}:pause")
    fun pause(@PathVariable id: Long): ReleaseDto = service.pause(id)

    /**
     * 恢复：从暂停状态恢复发布（与发布相同的前置校验）。
     */
    @Operation(summary = "恢复发布")
    @PostMapping("/{id}:resume")
    fun resume(@PathVariable id: Long): ReleaseDto = service.resume(id)

    /**
     * 获取制品上传/下载的预签名信息。
     *
     * 当前未接入对象存储预签名能力，因此固定返回 501，提示使用人工上传后调用 `artifact:complete` 或 `PATCH` 更新制品字段。
     *
     * @throws ResponseStatusException 501 固定抛出
     */
    @Operation(summary = "获取制品预签名地址（暂未实现）")
    @PostMapping("/{id}/artifact:presign")
    fun presignArtifact(@PathVariable id: Long): ArtifactPresignResponse {
        throw ResponseStatusException(
            HttpStatus.NOT_IMPLEMENTED,
            "OSS presign is not configured. Upload artifact manually and call POST /api/releases/{id}/artifact:complete (or PATCH /api/releases/{id}).",
        )
    }

    /**
     * 补全/更新制品信息（artifact 完成回调）。
     */
    @Operation(summary = "补全制品信息")
    @PostMapping("/{id}/artifact:complete")
    fun completeArtifact(@PathVariable id: Long, @Valid @RequestBody request: CompleteArtifactRequest): ReleaseDto =
        service.completeArtifact(id, request)
}
