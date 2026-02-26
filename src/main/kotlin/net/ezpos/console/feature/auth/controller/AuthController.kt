package net.ezpos.console.feature.auth.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import net.ezpos.console.feature.auth.dto.LoginRequest
import net.ezpos.console.feature.auth.dto.LoginResponse
import net.ezpos.console.feature.auth.dto.PlatformUserInfo
import net.ezpos.console.feature.auth.service.AuthService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(request: HttpServletRequest) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return
        val token = header.removePrefix("Bearer ").trim()
        authService.logout(token)
    }
}

