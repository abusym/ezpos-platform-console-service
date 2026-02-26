package net.ezpos.console.feature.user.security

import net.ezpos.console.common.security.model.PlatformUserPrincipal
import net.ezpos.console.common.security.spi.PrincipalLoader
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.springframework.stereotype.Service

@Service
class UserPrincipalLoader(
    private val repo: PlatformUserRepository,
) : PrincipalLoader {
    override fun load(userId: Long): PlatformUserPrincipal? {
        val user = repo.findById(userId).orElse(null) ?: return null
        if (!user.enabled) return null

        return PlatformUserPrincipal(
            id = userId,
            passwordHash = user.passwordHash,
            enabled = user.enabled,
            usernameValue = user.username,
        )
    }
}

