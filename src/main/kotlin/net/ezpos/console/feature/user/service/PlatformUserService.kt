package net.ezpos.console.feature.user.service

import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.feature.user.dto.CreatePlatformUserRequest
import net.ezpos.console.feature.user.dto.PlatformUserDto
import net.ezpos.console.feature.user.entity.PlatformUser
import net.ezpos.console.feature.user.mapper.PlatformUserMapper
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class PlatformUserService(
    private val repo: PlatformUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mapper: PlatformUserMapper,
) {
    fun create(request: CreatePlatformUserRequest): PlatformUserDto {
        if (repo.existsByUsername(request.username)) {
            throw EntityAlreadyExistsException("Username already exists")
        }

        val user = PlatformUser(
            username = request.username,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)) { "PasswordEncoder.encode returned null" },
            displayName = request.displayName,
            email = request.email,
            enabled = request.enabled,
        )

        val saved = repo.save(user)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }
}
