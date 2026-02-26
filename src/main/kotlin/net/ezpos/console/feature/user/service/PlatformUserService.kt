package net.ezpos.console.feature.user.service

import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.user.dto.ChangePasswordRequest
import net.ezpos.console.feature.user.dto.CreatePlatformUserRequest
import net.ezpos.console.feature.user.dto.PlatformUserDto
import net.ezpos.console.feature.user.dto.UpdatePlatformUserRequest
import net.ezpos.console.feature.user.entity.PlatformUser
import net.ezpos.console.feature.user.mapper.PlatformUserMapper
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    fun list(pageable: Pageable): Page<PlatformUserDto> =
        repo.findAll(pageable).map { mapper.toDto(requireNotNull(it.id), it) }

    fun getById(id: Long): PlatformUserDto {
        val user = repo.findById(id)
            .orElseThrow { EntityNotFoundException("PlatformUser", id) }
        return mapper.toDto(requireNotNull(user.id), user)
    }

    fun update(id: Long, request: UpdatePlatformUserRequest): PlatformUserDto {
        val user = repo.findById(id)
            .orElseThrow { EntityNotFoundException("PlatformUser", id) }

        request.displayName?.let { user.displayName = it.trim().ifEmpty { null } }
        request.email?.let { user.email = it.trim().ifEmpty { null } }
        request.enabled?.let { user.enabled = it }

        val saved = repo.save(user)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun changePassword(id: Long, request: ChangePasswordRequest) {
        val user = repo.findById(id)
            .orElseThrow { EntityNotFoundException("PlatformUser", id) }

        if (!passwordEncoder.matches(request.oldPassword, user.passwordHash)) {
            throw BusinessRuleException("Old password is incorrect")
        }

        user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
        repo.save(user)
    }
}
