package net.ezpos.console.feature.merchant.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

@Entity
@Table(
    name = "merchants",
    indexes = [
        Index(name = "idx_merchants_enabled", columnList = "enabled"),
        Index(name = "idx_merchants_created_at", columnList = "created_at"),
    ],
)
class Merchant(
    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "contact_name", length = 64)
    var contactName: String? = null,

    @Column(name = "contact_phone", length = 32)
    var contactPhone: String? = null,

    @Column(name = "address", length = 512)
    var address: String? = null,

    @Column(name = "memo", length = 1000)
    var memo: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
) : IdEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}
