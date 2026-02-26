package net.ezpos.console.common.security.current

import net.ezpos.console.common.security.model.PlatformUserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * 基于 Spring Security `SecurityContextHolder` 的 [CurrentPrincipalProvider] 实现。
 *
 * - 若当前请求未认证，则 `principalOrNull()` 返回 `null`
 * - 若当前请求已认证但 principal 类型非 [PlatformUserPrincipal]，同样返回 `null`
 */
@Service
class SpringSecurityCurrentPrincipalProvider : CurrentPrincipalProvider {
    override fun principalOrNull(): PlatformUserPrincipal? {
        val auth = SecurityContextHolder.getContext()?.authentication ?: return null
        return auth.principal as? PlatformUserPrincipal
    }

    override fun requirePrincipal(): PlatformUserPrincipal =
        principalOrNull() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated")
}

