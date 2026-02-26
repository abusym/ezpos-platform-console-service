package net.ezpos.console.common.entity.id

import org.hibernate.annotations.IdGeneratorType

/**
 * Snowflake 主键生成注解（用于 JPA/Hibernate 的 `@Id` 字段）。
 *
 * ## 背景
 * 该注解通过 Hibernate 6 的 `@IdGeneratorType` 机制把某个字段标记为“由自定义生成器生成”。
 *
 * ## 用法
 * 在实体主键字段上同时标注 `@Id` 与 `@SnowflakeId`：
 *
 * ```
 * @Id
 * @SnowflakeId
 * val id: Long? = null
 * ```
 *
 * 生成逻辑由 [SnowflakeIdGenerator] 提供。
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@IdGeneratorType(SnowflakeIdGenerator::class)
annotation class SnowflakeId

