package net.ezpos.console.feature.audit.repository

import net.ezpos.console.feature.audit.entity.AuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long>
