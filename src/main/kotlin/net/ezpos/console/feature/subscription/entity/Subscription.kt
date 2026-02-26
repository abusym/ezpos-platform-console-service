package net.ezpos.console.feature.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import net.ezpos.console.common.entity.base.IdEntity
import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
    name = "subscriptions",
    indexes = [
        Index(name = "idx_subscriptions_merchant_id", columnList = "merchant_id"),
        Index(name = "idx_subscriptions_status", columnList = "status"),
        Index(name = "idx_subscriptions_end_date", columnList = "end_date"),
    ],
)
class Subscription(
    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long,

    @Column(name = "plan_id", nullable = false)
    var planId: Long,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,

    @Column(name = "renewed_at", nullable = true, columnDefinition = "timestamptz")
    var renewedAt: OffsetDateTime? = null,
) : IdEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}
