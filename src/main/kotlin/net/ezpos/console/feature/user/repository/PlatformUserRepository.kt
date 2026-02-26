package net.ezpos.console.feature.user.repository

import net.ezpos.console.feature.user.entity.PlatformUser
import org.springframework.data.jpa.repository.JpaRepository

interface PlatformUserRepository : JpaRepository<PlatformUser, Long> {
    fun findByUsername(username: String): PlatformUser?
    fun existsByUsername(username: String): Boolean
}

