package net.ezpos.console.feature.user.controller

import jakarta.validation.Valid
import net.ezpos.console.feature.user.dto.ChangePasswordRequest
import net.ezpos.console.feature.user.dto.CreatePlatformUserRequest
import net.ezpos.console.feature.user.dto.PlatformUserDto
import net.ezpos.console.feature.user.dto.UpdatePlatformUserRequest
import net.ezpos.console.feature.user.service.PlatformUserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/platform-users")
class PlatformUsersController(
    private val platformUserService: PlatformUserService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlatformUserRequest): PlatformUserDto =
        platformUserService.create(request)

    @GetMapping
    fun list(pageable: Pageable): Page<PlatformUserDto> =
        platformUserService.list(pageable)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): PlatformUserDto =
        platformUserService.getById(id)

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdatePlatformUserRequest): PlatformUserDto =
        platformUserService.update(id, request)

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@PathVariable id: Long, @Valid @RequestBody request: ChangePasswordRequest) {
        platformUserService.changePassword(id, request)
    }
}
