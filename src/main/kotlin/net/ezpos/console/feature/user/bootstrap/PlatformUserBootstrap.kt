package net.ezpos.console.feature.user.bootstrap

import net.ezpos.console.feature.user.entity.PlatformUser
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class PlatformUserBootstrap(
    private val repo: PlatformUserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val username = "admin"
        if (repo.existsByUsername(username)) return

        val user = PlatformUser(
            username = username,
            passwordHash = requireNotNull(passwordEncoder.encode("123456")) { "PasswordEncoder.encode returned null" },
            displayName = "Administrator",
            email = null,
            enabled = true,
        )

        repo.save(user)
        log.info("Bootstrapped initial platform user: username={}", username)
    }
}

