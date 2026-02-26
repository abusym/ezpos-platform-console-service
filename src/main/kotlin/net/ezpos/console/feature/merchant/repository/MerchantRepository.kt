package net.ezpos.console.feature.merchant.repository

import net.ezpos.console.feature.merchant.entity.Merchant
import org.springframework.data.jpa.repository.JpaRepository

interface MerchantRepository : JpaRepository<Merchant, Long>
