package net.ezpos.console.feature.migration.service

import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.migration.dto.CreateDataMigrationRequest
import net.ezpos.console.feature.migration.dto.DataMigrationDto
import net.ezpos.console.feature.migration.entity.DataMigration
import net.ezpos.console.feature.migration.mapper.DataMigrationMapper
import net.ezpos.console.feature.migration.model.MigrationStatus
import net.ezpos.console.feature.migration.model.MigrationType
import net.ezpos.console.feature.migration.repository.DataMigrationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class DataMigrationService(
    private val repo: DataMigrationRepository,
    private val mapper: DataMigrationMapper,
) {
    fun create(request: CreateDataMigrationRequest): DataMigrationDto {
        val migration = DataMigration(
            title = request.title,
            description = request.description,
            sourceMerchantId = request.sourceMerchantId,
            targetMerchantId = request.targetMerchantId,
            type = MigrationType.fromJson(request.type),
            status = MigrationStatus.PENDING,
            progress = 0,
        )

        val saved = repo.save(migration)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun list(pageable: Pageable): Page<DataMigrationDto> =
        repo.findAll(pageable).map { mapper.toDto(requireNotNull(it.id), it) }

    fun getById(id: Long): DataMigrationDto {
        val migration = repo.findById(id)
            .orElseThrow { EntityNotFoundException("DataMigration", id) }
        return mapper.toDto(requireNotNull(migration.id), migration)
    }
}
