package net.ezpos.console.feature.subscription.repository

import net.ezpos.console.feature.subscription.entity.Plan
import org.springframework.data.jpa.repository.JpaRepository

interface PlanRepository : JpaRepository<Plan, Long>
