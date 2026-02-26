package net.ezpos.console.feature.migration.controller

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

@RestController
@RequestMapping("/api/data-migrations")
class DataMigrationsController(
    private val dataMigrationService: DataMigrationService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateDataMigrationRequest): DataMigrationDto =
        dataMigrationService.create(request)

    @GetMapping
    fun list(pageable: Pageable): Page<DataMigrationDto> =
        dataMigrationService.list(pageable)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): DataMigrationDto =
        dataMigrationService.getById(id)
}
