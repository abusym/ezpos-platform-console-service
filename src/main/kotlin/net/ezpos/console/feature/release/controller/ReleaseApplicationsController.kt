package net.ezpos.console.feature.release.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import net.ezpos.console.feature.release.dto.CreateReleaseApplicationRequest
import net.ezpos.console.feature.release.dto.ReleaseApplicationDto
import net.ezpos.console.feature.release.dto.UpdateReleaseApplicationRequest
import net.ezpos.console.feature.release.service.ReleaseApplicationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 可发布应用（Release Application）管理接口（控制台侧）。
 *
 * Base path: `/api/release-applications`
 *
 * 用于维护应用主数据（code/name/description/enabled），以便后续按应用维度配置发布信息。
 */
@Tag(name = "应用管理", description = "可发布应用的增删改查")
@RestController
@RequestMapping("/api/release-applications")
class ReleaseApplicationsController(
    private val service: ReleaseApplicationService,
) {
    /**
     * 列出所有可发布应用。
     */
    @Operation(summary = "查询所有应用")
    @GetMapping
    fun list(): List<ReleaseApplicationDto> = service.list()

    /**
     * 按应用编码获取详情。
     */
    @Operation(summary = "获取应用详情")
    @GetMapping("/{code}")
    fun get(@PathVariable code: String): ReleaseApplicationDto = service.getByCode(code)

    /**
     * 创建新的可发布应用。
     */
    @Operation(summary = "创建应用")
    @PostMapping
    fun create(@Valid @RequestBody request: CreateReleaseApplicationRequest): ReleaseApplicationDto = service.create(request)

    /**
     * 按编码局部更新应用信息。
     */
    @Operation(summary = "更新应用")
    @PatchMapping("/{code}")
    fun update(
        @PathVariable code: String,
        @Valid @RequestBody request: UpdateReleaseApplicationRequest,
    ): ReleaseApplicationDto = service.update(code, request)
}
