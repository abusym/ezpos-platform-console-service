package net.ezpos.console.common.security.spi

/**
 * Token 解析（Introspection）SPI。
 *
 * ## 目的
 * 将“token 如何验证/解析”从 Web Filter 与业务模块中解耦出来：
 * - Filter 只负责从请求中提取 token、并将解析结果写入 Spring Security Context
 * - token 的具体实现（opaque token、JWT、远程 introspection 等）由该接口的实现类决定
 *
 * ## 约定
 * - 返回 `null` 表示 token 无效/过期/无法解析
 * - 返回用户 ID 表示 token 有效且已映射到某个用户
 */
interface TokenIntrospector {
    /**
     * 解析 token 并返回用户 ID。
     *
     * @param token Bearer token（已去掉 "Bearer " 前缀）
     * @return 用户 ID；若 token 无效则返回 `null`
     */
    fun resolveUserId(token: String): Long?
}

