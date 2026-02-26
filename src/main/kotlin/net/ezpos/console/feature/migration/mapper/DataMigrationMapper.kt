package net.ezpos.console.feature.migration.mapper

import net.ezpos.console.feature.migration.dto.DataMigrationDto
import net.ezpos.console.feature.migration.entity.DataMigration
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface DataMigrationMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(expression = "java(entity.getType().getValue())", target = "type")
    @Mapping(expression = "java(entity.getStatus().getValue())", target = "status")
    fun toDto(id: Long, entity: DataMigration): DataMigrationDto
}
