package net.ezpos.console.feature.migration.repository

import net.ezpos.console.feature.migration.entity.DataMigration
import org.springframework.data.jpa.repository.JpaRepository

interface DataMigrationRepository : JpaRepository<DataMigration, Long>
