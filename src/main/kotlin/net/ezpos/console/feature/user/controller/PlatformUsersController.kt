package net.ezpos.console.feature.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "平台用户", description = "平台用户的增删改查与密码管理")
@RestController
@RequestMapping("/api/platform-users")
class PlatformUsersController(
    private val platformUserService: PlatformUserService,
) {
    @Operation(summary = "创建平台用户")
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlatformUserRequest): PlatformUserDto =
        platformUserService.create(request)

    @Operation(summary = "分页查询平台用户")
    @GetMapping
    fun list(pageable: Pageable): Page<PlatformUserDto> =
        platformUserService.list(pageable)

    @Operation(summary = "获取平台用户详情")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): PlatformUserDto =
        platformUserService.getById(id)

    @Operation(summary = "更新平台用户")
    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdatePlatformUserRequest): PlatformUserDto =
        platformUserService.update(id, request)

    @Operation(summary = "修改密码")
    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@PathVariable id: Long, @Valid @RequestBody request: ChangePasswordRequest) {
        platformUserService.changePassword(id, request)
    }
}
