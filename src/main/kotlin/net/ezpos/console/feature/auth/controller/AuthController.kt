package net.ezpos.console.feature.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "认证", description = "登录、登出、获取当前用户信息")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(summary = "登录")
    @SecurityRequirements
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    fun me(): PlatformUserInfo = authService.me()

    @Operation(summary = "登出")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(request: HttpServletRequest) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return
        val token = header.removePrefix("Bearer ").trim()
        authService.logout(token)
    }
}
