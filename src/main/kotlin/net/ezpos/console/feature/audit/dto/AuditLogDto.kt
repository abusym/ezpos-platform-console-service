package net.ezpos.console.feature.audit.dto

import java.time.OffsetDateTime

data class AuditLogDto(
    val id: Long,
    val userId: Long? = null,
    val username: String? = null,
    val action: String,
    val resourceType: String,
    val resourceId: String? = null,
    val detail: String? = null,
    val ipAddress: String? = null,
    val createdAt: OffsetDateTime? = null,
)
