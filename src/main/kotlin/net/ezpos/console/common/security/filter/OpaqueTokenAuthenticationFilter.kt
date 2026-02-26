package net.ezpos.console.common.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.ezpos.console.common.security.spi.PrincipalLoader
import net.ezpos.console.common.security.spi.TokenIntrospector
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 基于 Opaque Token 的无状态认证 Filter。
 *
 * ## 工作方式
 * - 从请求头 `Authorization: Bearer <token>` 提取 token
 * - 通过 [TokenIntrospector] 将 token 解析为 userId（例如从 Redis 查映射）
 * - 通过 [PrincipalLoader] 加载 [net.ezpos.console.common.security.model.PlatformUserPrincipal]
 * - 构造 `UsernamePasswordAuthenticationToken` 写入 `SecurityContext`
 *
 * ## 失败行为
 * - 若 token 缺失：放行（交由后续的 Spring Security 授权规则决定是否需要认证）
 * - 若 token 无效/过期：直接返回 401（避免进入业务 Controller）
 *
 * ## 放行路径
 * 登录与 API 文档相关路径会跳过该 Filter（见 [shouldNotFilter]）。
 */
class OpaqueTokenAuthenticationFilter(
    private val tokenIntrospector: TokenIntrospector,
    private val principalLoader: PrincipalLoader,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {
    /**
     * 是否跳过本 Filter。
     *
     * 用于放行无需认证的公共端点（登录、Swagger/OpenAPI 文档）。
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath ?: ""
        return path == "/api/auth/login" ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path == "/swagger-ui.html"
    }

    /**
     * 执行认证逻辑：
     * - 提取 Bearer token
     * - introspect -> userId
     * - load principal
     * - 写入 SecurityContext
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith("Bearer ", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substring(7).trim()
        if (token.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val userId = tokenIntrospector.resolveUserId(token)
        if (userId == null) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(
                request,
                response,
                BadCredentialsException("Invalid or expired access token"),
            )
            return
        }

        val principal = principalLoader.load(userId)
        if (principal == null) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(
                request,
                response,
                BadCredentialsException("Invalid or expired access token"),
            )
            return
        }

        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = auth

        filterChain.doFilter(request, response)
    }
}

