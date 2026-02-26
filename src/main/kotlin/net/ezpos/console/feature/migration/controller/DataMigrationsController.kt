package net.ezpos.console.feature.migration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import net.ezpos.console.feature.migration.dto.CreateDataMigrationRequest
import net.ezpos.console.feature.migration.dto.DataMigrationDto
import net.ezpos.console.feature.migration.service.DataMigrationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "数据迁移", description = "数据迁移任务的创建与查询")
@RestController
@RequestMapping("/api/data-migrations")
class DataMigrationsController(
    private val dataMigrationService: DataMigrationService,
) {
    @Operation(summary = "创建迁移任务")
    @PostMapping
    fun create(@Valid @RequestBody request: CreateDataMigrationRequest): DataMigrationDto =
        dataMigrationService.create(request)

    @Operation(summary = "分页查询迁移任务")
    @GetMapping
    fun list(pageable: Pageable): Page<DataMigrationDto> =
        dataMigrationService.list(pageable)

    @Operation(summary = "获取迁移任务详情")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): DataMigrationDto =
        dataMigrationService.getById(id)
}
