package net.ezpos.console.feature.migration.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity
import net.ezpos.console.feature.migration.model.MigrationStatus
import net.ezpos.console.feature.migration.model.MigrationType
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

@Entity
@Table(
    name = "data_migrations",
    indexes = [
        Index(name = "idx_data_migrations_status", columnList = "status"),
        Index(name = "idx_data_migrations_created_at", columnList = "created_at"),
    ],
)
class DataMigration(
    @Column(name = "title", nullable = false, length = 128)
    var title: String,

    @Column(name = "description", nullable = true, length = 1000)
    var description: String? = null,

    @Column(name = "source_merchant_id", nullable = true)
    var sourceMerchantId: Long? = null,

    @Column(name = "target_merchant_id", nullable = true)
    var targetMerchantId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    var type: MigrationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: MigrationStatus = MigrationStatus.PENDING,

    @Column(name = "progress", nullable = false)
    var progress: Int = 0,

    @Column(name = "error_message", nullable = true, length = 2000)
    var errorMessage: String? = null,

    @Column(name = "started_at", nullable = true, columnDefinition = "timestamptz")
    var startedAt: OffsetDateTime? = null,

    @Column(name = "completed_at", nullable = true, columnDefinition = "timestamptz")
    var completedAt: OffsetDateTime? = null,
) : IdEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}
