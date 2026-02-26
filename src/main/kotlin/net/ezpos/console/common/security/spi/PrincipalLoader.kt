package net.ezpos.console.common.security.spi

import net.ezpos.console.common.security.model.PlatformUserPrincipal

/**
 * Principal 加载 SPI（由 userId -> [PlatformUserPrincipal]）。
 *
 * ## 目的
 * 将“如何从用户存储加载用户信息/状态”从鉴权 Filter 中隔离出来：
 * - Filter 不关心用户表结构、禁用逻辑、权限装配等细节
 * - 具体实现可以来自数据库、缓存、外部用户中心等
 *
 * ## 约定
 * - 返回 `null` 表示该 userId 不存在或不可用于登录（例如被禁用）
 */
interface PrincipalLoader {
    /**
     * 根据用户 ID 加载 principal。
     *
     * @param userId 由 token 解析得到的用户 ID
     * @return 可用于 Spring Security 的 principal；若不可用返回 `null`
     */
    fun load(userId: Long): PlatformUserPrincipal?
}

