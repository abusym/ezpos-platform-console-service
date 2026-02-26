package net.ezpos.console.feature.auth.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.ezpos.console.common.exception.AuthenticationFailedException
import net.ezpos.console.common.security.current.CurrentPrincipalProvider
import net.ezpos.console.feature.auth.mapper.PlatformUserInfoMapper
import net.ezpos.console.feature.auth.token.OpaqueTokenService
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows

class AuthServiceTest {

    private val repo = mockk<PlatformUserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val tokenService = mockk<OpaqueTokenService>(relaxUnitFun = true)
    private val currentPrincipal = mockk<CurrentPrincipalProvider>()
    private val userInfoMapper = mockk<PlatformUserInfoMapper>()
    private val service = AuthService(repo, passwordEncoder, tokenService, currentPrincipal, userInfoMapper)

    // ── logout ──

    @Test
    fun `logout revokes the given token`() {
        val token = "test-token-abc"
        service.logout(token)
        verify(exactly = 1) { tokenService.revoke(token) }
    }

    // ── login ──

    @Test
    fun `login throws AuthenticationFailedException when user not found`() {
        every { repo.findByUsername("unknown") } returns null
        assertThrows<AuthenticationFailedException> {
            service.login(net.ezpos.console.feature.auth.dto.LoginRequest("unknown", "pass"))
        }
    }
}
