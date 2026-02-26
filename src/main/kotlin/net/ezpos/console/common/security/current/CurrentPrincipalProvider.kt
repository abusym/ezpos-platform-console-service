package net.ezpos.console.common.security.current

import net.ezpos.console.common.security.model.PlatformUserPrincipal

/**
 * “当前登录用户”的访问入口（可注入）。
 *
 * ## 为什么需要它
 * 在业务代码中直接使用 `SecurityContextHolder` 会导致：
 * - 各处重复的类型转换/空判断/异常处理
 * - 业务代码与 Spring Security 线程上下文强耦合，难测且不易演进
 *
 * 该 Provider 将这些细节集中封装，业务层可以更简洁地获取：
 * - 当前请求的 [PlatformUserPrincipal]
 * - 当前用户 ID（`principal.id`）
 *
 * ## 适用范围
 * 仅适用于同一次 HTTP 请求链路（同线程）内。若在异步/线程切换后使用，需要显式传播 SecurityContext。
 */
interface CurrentPrincipalProvider {
    /** 返回当前请求的 principal；若未认证则返回 `null`。 */
    fun principalOrNull(): PlatformUserPrincipal?

    /** 返回当前请求的 principal；若未认证则抛出 401。 */
    fun requirePrincipal(): PlatformUserPrincipal

    /** 返回当前用户 ID；若未认证则返回 `null`。 */
    fun userIdOrNull(): Long? = principalOrNull()?.id

    /** 返回当前用户 ID；若未认证则抛出 401。 */
    fun requireUserId(): Long = requirePrincipal().id
}

