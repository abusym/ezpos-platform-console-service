package net.ezpos.console.feature.release.repository

import net.ezpos.console.feature.release.entity.ReleaseApplication
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [ReleaseApplication] 的数据访问层（Spring Data JPA）。
 *
 * 用于通过应用 `code` 做存在性校验与按编码查询。
 */
interface ReleaseApplicationRepository : JpaRepository<ReleaseApplication, Long> {
    /**
     * 判断指定应用编码是否已存在。
     */
    fun existsByCode(code: String): Boolean

    /**
     * 按应用编码查询。
     *
     * @return 若不存在则返回 `null`
     */
    fun findByCode(code: String): ReleaseApplication?
}

