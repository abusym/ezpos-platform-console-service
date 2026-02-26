package net.ezpos.console.feature.release.repository

import net.ezpos.console.feature.release.entity.ClientUpdateReport
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [ClientUpdateReport] 的数据访问层（Spring Data JPA）。
 */
interface ClientUpdateReportRepository : JpaRepository<ClientUpdateReport, Long>
