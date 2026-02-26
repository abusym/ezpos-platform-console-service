package net.ezpos.console.feature.audit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity

@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
        Index(name = "idx_audit_logs_action", columnList = "action"),
        Index(name = "idx_audit_logs_resource_type", columnList = "resource_type"),
        Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
    ],
)
class AuditLog(
    @Column(name = "user_id", nullable = true)
    var userId: Long? = null,

    @Column(name = "username", nullable = true, length = 64)
    var username: String? = null,

    @Column(name = "action", nullable = false, length = 64)
    var action: String,

    @Column(name = "resource_type", nullable = false, length = 64)
    var resourceType: String,

    @Column(name = "resource_id", nullable = true, length = 128)
    var resourceId: String? = null,

    @Column(name = "detail", nullable = true, length = 2000)
    var detail: String? = null,

    @Column(name = "ip_address", nullable = true, length = 45)
    var ipAddress: String? = null,
) : IdEntity()
