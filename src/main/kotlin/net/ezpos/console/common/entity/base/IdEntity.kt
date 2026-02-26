package net.ezpos.console.common.entity.base

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import net.ezpos.console.common.entity.id.SnowflakeId
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

/**
 * 所有 JPA Entity 的基础基类（带主键与创建时间）。
 *
 * ## 设计要点
 * - **统一主键策略**：使用 Snowflake 生成的 `Long` 类型 ID，避免数据库自增 ID 在分库分表/多实例下的协调成本。
 * - **字段访问（FIELD access）**：通过 `@Access(AccessType.FIELD)` 强制 Hibernate 直接操作字段。
 *
 * ## 使用方式
 * 业务实体继承本类即可获得：
 * - `id`：主键（由 `@SnowflakeId` 生成）
 * - `createdAt`：创建时间（由 Hibernate 自动填充）
 */
@MappedSuperclass
@Access(AccessType.FIELD)
abstract class IdEntity {

    /**
     * 主键 ID。
     *
     * - 由 Hibernate 在持久化时通过 `@SnowflakeId` 注解对应的生成器生成并回填
     * - `updatable = false` 确保主键不可变
     *
     * 注意：在实体尚未持久化前该值为 `null`。
     */
    @Id
    @SnowflakeId
    @Column(name = "id", nullable = false, updatable = false)
    var id: Long? = null

    /**
     * 记录创建时间（带时区）。
     *
     * - 由 Hibernate 在插入时自动填充
     * - `updatable = false` 确保创建时间不会被二次更新覆盖
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    var createdAt: OffsetDateTime? = null
}

