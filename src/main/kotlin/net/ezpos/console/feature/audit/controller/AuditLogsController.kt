package net.ezpos.console.feature.audit.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import net.ezpos.console.feature.audit.dto.AuditLogDto
import net.ezpos.console.feature.audit.service.AuditLogService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "审计日志", description = "操作审计日志查询")
@RestController
@RequestMapping("/api/audit-logs")
class AuditLogsController(
    private val auditLogService: AuditLogService,
) {
    @Operation(summary = "分页查询审计日志")
    @GetMapping
    fun list(pageable: Pageable): Page<AuditLogDto> =
        auditLogService.list(pageable)
}
