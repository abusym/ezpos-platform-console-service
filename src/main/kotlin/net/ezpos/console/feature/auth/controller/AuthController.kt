package net.ezpos.console.feature.auth.controller

import jakarta.validation.Valid
import net.ezpos.console.feature.auth.dto.LoginRequest
import net.ezpos.console.feature.auth.dto.LoginResponse
import net.ezpos.console.feature.auth.dto.PlatformUserInfo
import net.ezpos.console.feature.auth.service.AuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    @GetMapping("/me")
    fun me(): PlatformUserInfo = authService.me()
}

