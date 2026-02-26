package net.ezpos.console.feature.release.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import net.ezpos.console.common.entity.base.IdEntity
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

/**
 * 可发布应用的主数据（Release Application）。
 *
 * 用于维护哪些“应用”允许在控制台配置发布信息，以及向外暴露时显示名称/描述等元信息。
 *
 * @property code 应用编码（唯一；用于与 [net.ezpos.console.feature.release.entity.Release.applicationCode] 关联）
 * @property name 应用名称（用于展示）
 * @property description 应用描述（可为空）
 * @property enabled 是否启用：禁用后通常不允许创建/更新发布配置或对外提供更新检查
 */
@Entity
@Table(
    name = "console_release_applications",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_console_release_applications_code", columnNames = ["code"]),
    ],
    indexes = [
        Index(name = "idx_console_release_applications_code", columnList = "code"),
        Index(name = "idx_console_release_applications_enabled", columnList = "enabled"),
    ],
)
class ReleaseApplication(
    @Column(name = "code", nullable = false, length = 64)
    var code: String,

    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "description", nullable = true, length = 512)
    var description: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
) : IdEntity() {
    /**
     * 最近一次更新时间（由 Hibernate 自动维护）。
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}

