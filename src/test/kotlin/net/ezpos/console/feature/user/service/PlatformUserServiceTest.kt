package net.ezpos.console.feature.user.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.ezpos.console.common.exception.BusinessRuleException
import net.ezpos.console.common.exception.EntityAlreadyExistsException
import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.user.dto.ChangePasswordRequest
import net.ezpos.console.feature.user.dto.CreatePlatformUserRequest
import net.ezpos.console.feature.user.dto.UpdatePlatformUserRequest
import net.ezpos.console.feature.user.entity.PlatformUser
import net.ezpos.console.feature.user.mapper.PlatformUserMapper
import net.ezpos.console.feature.user.repository.PlatformUserRepository
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformUserServiceTest {

    private val repo = mockk<PlatformUserRepository>(relaxed = true)
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val mapper = mockk<PlatformUserMapper>()
    private val service = PlatformUserService(repo, passwordEncoder, mapper)

    // ── create ──

    @Test
    fun `create throws EntityAlreadyExistsException when username exists`() {
        every { repo.existsByUsername("admin") } returns true
        assertThrows<EntityAlreadyExistsException> {
            service.create(CreatePlatformUserRequest("admin", "pass"))
        }
    }

    // ── getById ──

    @Test
    fun `getById throws EntityNotFoundException when user not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.getById(999L)
        }
    }

    // ── update ──

    @Test
    fun `update modifies displayName and email`() {
        val user = aUser()
        every { repo.findById(1L) } returns Optional.of(user)
        every { repo.save(user) } returns user
        every { mapper.toDto(1L, user) } returns mockk()

        service.update(1L, UpdatePlatformUserRequest(displayName = "New Name", email = "new@test.com"))

        assertEquals("New Name", user.displayName)
        assertEquals("new@test.com", user.email)
    }

    @Test
    fun `update throws EntityNotFoundException when user not found`() {
        every { repo.findById(999L) } returns Optional.empty()
        assertThrows<EntityNotFoundException> {
            service.update(999L, UpdatePlatformUserRequest(displayName = "x"))
        }
    }

    // ── changePassword ──

    @Test
    fun `changePassword succeeds with correct old password`() {
        val user = aUser()
        every { repo.findById(1L) } returns Optional.of(user)
        every { passwordEncoder.matches("old123", "hashed") } returns true
        every { passwordEncoder.encode("new456") } returns "newHash"
        every { repo.save(user) } returns user

        service.changePassword(1L, ChangePasswordRequest("old123", "new456"))

        assertEquals("newHash", user.passwordHash)
        verify { repo.save(user) }
    }

    @Test
    fun `changePassword throws BusinessRuleException with wrong old password`() {
        val user = aUser()
        every { repo.findById(1L) } returns Optional.of(user)
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        assertThrows<BusinessRuleException> {
            service.changePassword(1L, ChangePasswordRequest("wrong", "new456"))
        }
    }

    private fun aUser() = PlatformUser(
        username = "testuser",
        passwordHash = "hashed",
        displayName = "Test",
        email = "test@test.com",
        enabled = true,
    ).apply { id = 1L }
}
