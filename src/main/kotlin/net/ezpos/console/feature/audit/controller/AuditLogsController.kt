package net.ezpos.console.feature.audit.controller

import net.ezpos.console.feature.audit.dto.AuditLogDto
import net.ezpos.console.feature.audit.service.AuditLogService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/audit-logs")
class AuditLogsController(
    private val auditLogService: AuditLogService,
) {
    @GetMapping
    fun list(pageable: Pageable): Page<AuditLogDto> =
        auditLogService.list(pageable)
}
