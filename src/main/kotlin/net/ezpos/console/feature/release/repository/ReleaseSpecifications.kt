package net.ezpos.console.feature.release.repository

import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.model.ReleaseStatus
import org.springframework.data.jpa.domain.Specification

/**
 * [Release] 列表查询的条件组合（JPA [Specification]）。
 *
 * 用于把可选过滤条件（applicationCode/platform/status）组装成一个 [Specification]，
 * 供 Repository 的 `findAll(spec, pageable)` 使用。
 */
object ReleaseSpecifications {
    /**
     * 构造 Release 列表的过滤条件。
     *
     * - `applicationCode` / `platform` 会先 `trim()`；空白视为“不过滤”
     * - `status` 为 `null` 视为“不过滤”
     */
    fun filter(
        applicationCode: String?,
        platform: String?,
        status: ReleaseStatus?,
    ): Specification<Release> {
        val app = applicationCode?.trim()?.takeIf { it.isNotEmpty() }
        val p = platform?.trim()?.takeIf { it.isNotEmpty() }

        return Specification { root, _, cb ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
            if (app != null) predicates += cb.equal(root.get<String>("applicationCode"), app)
            if (p != null) predicates += cb.equal(root.get<String>("platform"), p)
            if (status != null) predicates += cb.equal(root.get<ReleaseStatus>("status"), status)
            cb.and(*predicates.toTypedArray())
        }
    }
}

