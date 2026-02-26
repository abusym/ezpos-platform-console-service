package net.ezpos.console.feature.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "plans")
class Plan(
    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "description", nullable = true, length = 512)
    var description: String? = null,

    @Column(name = "duration_days", nullable = false)
    var durationDays: Int,

    @Column(name = "price", nullable = false)
    var price: Long,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
) : IdEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}
