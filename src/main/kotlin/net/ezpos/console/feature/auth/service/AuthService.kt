package net.ezpos.console.feature.auth.service

import net.ezpos.console.common.exception.AuthenticationFailedException
import net.ezpos.console.common.security.current.CurrentPrincipalProvider
import net.ezpos.console.feature.auth.dto.LoginRequest
import net.ezpos.console.feature.auth.dto.LoginResponse
import net.ezpos.console.feature.auth.dto.PlatformUserInfo
import net.ezpos.console.feature.auth.mapper.PlatformUserInfoMapper
import net.ezpos.console.feature.auth.token.OpaqueTokenService
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val repo: PlatformUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: OpaqueTokenService,
    private val currentPrincipal: CurrentPrincipalProvider,
    private val userInfoMapper: PlatformUserInfoMapper,
) {
    fun login(request: LoginRequest): LoginResponse {
        val user = repo.findByUsername(request.username)
            ?: throw AuthenticationFailedException("Invalid credentials")

        if (!user.enabled) {
            throw AuthenticationFailedException("User disabled")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthenticationFailedException("Invalid credentials")
        }

        val accessToken = tokenService.issueAccessToken(requireNotNull(user.id))
        return LoginResponse(
            accessToken = accessToken,
            expiresInSeconds = tokenService.ttlSeconds(),
            user = userInfoMapper.fromUser(requireNotNull(user.id), user),
        )
    }

    /**
     * 获取当前登录平台用户的信息（用于 `/api/auth/me`）。
     *
     * 当前实现以 SecurityContext 中的 principal 为准，并按需从数据库补全 displayName/email。
     */
    fun me(): PlatformUserInfo {
        val principal = currentPrincipal.requirePrincipal()
        val user = repo.findById(principal.id).orElse(null)
        return userInfoMapper.fromPrincipalAndUser(principal, user)
    }
}
