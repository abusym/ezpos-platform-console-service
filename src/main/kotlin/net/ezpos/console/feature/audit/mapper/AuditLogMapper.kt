package net.ezpos.console.feature.audit.mapper

import net.ezpos.console.feature.audit.dto.AuditLogDto
import net.ezpos.console.feature.audit.entity.AuditLog
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface AuditLogMapper {
    fun toDto(id: Long, entity: AuditLog): AuditLogDto
}
