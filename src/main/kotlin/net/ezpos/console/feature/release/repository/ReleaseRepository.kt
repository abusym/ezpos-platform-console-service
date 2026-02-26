package net.ezpos.console.feature.release.repository

import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.model.ReleaseStatus
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.JpaRepository

/**
 * [Release] 的数据访问层（Spring Data JPA）。
 *
 * 主要通过方法名派生查询，用于按应用/平台定位唯一的发布记录，以及按状态过滤。
 */
interface ReleaseRepository : JpaRepository<Release, Long>, JpaSpecificationExecutor<Release> {
    /**
     * 查询某应用在某平台上的发布配置。
     *
     * @return 若不存在则返回 `null`
     */
    fun findByApplicationCodeAndPlatform(applicationCode: String, platform: String): Release?

    /**
     * 查询某应用在某平台上，且处于指定 [status] 的发布配置。
     *
     * 常用于“仅在发布状态下才可对客户端生效”的场景。
     *
     * @return 若不存在则返回 `null`
     */
    fun findByApplicationCodeAndPlatformAndStatus(
        applicationCode: String,
        platform: String,
        status: ReleaseStatus,
    ): Release?
}

