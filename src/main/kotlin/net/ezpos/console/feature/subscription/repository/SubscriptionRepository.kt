package net.ezpos.console.feature.subscription.repository

import net.ezpos.console.feature.subscription.entity.Subscription
import net.ezpos.console.feature.subscription.model.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByEndDateBeforeAndStatus(date: LocalDate, status: SubscriptionStatus): List<Subscription>
}
