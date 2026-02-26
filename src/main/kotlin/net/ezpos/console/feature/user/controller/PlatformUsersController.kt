package net.ezpos.console.feature.user.controller

import jakarta.validation.Valid
import net.ezpos.console.feature.user.dto.CreatePlatformUserRequest
import net.ezpos.console.feature.user.dto.PlatformUserDto
import net.ezpos.console.feature.user.service.PlatformUserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/platform-users")
class PlatformUsersController(
    private val platformUserService: PlatformUserService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlatformUserRequest): PlatformUserDto =
        platformUserService.create(request)
}

