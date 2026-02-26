package net.ezpos.console.feature.audit.service

import net.ezpos.console.feature.audit.dto.AuditLogDto
import net.ezpos.console.feature.audit.mapper.AuditLogMapper
import net.ezpos.console.feature.audit.repository.AuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AuditLogService(
    private val repo: AuditLogRepository,
    private val mapper: AuditLogMapper,
) {
    fun list(pageable: Pageable): Page<AuditLogDto> =
        repo.findAll(pageable).map { mapper.toDto(requireNotNull(it.id), it) }
}
