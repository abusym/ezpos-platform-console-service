package net.ezpos.console.feature.release.controller

import net.ezpos.console.feature.release.dto.ClientUpdateCheckResponse
import net.ezpos.console.feature.release.service.ClientUpdateService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 客户端更新检查接口。
 *
 * Base path: `/api/client-updates`
 *
 * 该接口面向客户端调用，采用请求头传递维度信息（应用、平台、当前版本、租户、可选设备 id）。
 * 具体的版本比较与灰度命中判定由 [ClientUpdateService] 完成。
 */
@RestController
@RequestMapping("/api/client-updates")
class ClientUpdatesController(
    private val clientUpdateService: ClientUpdateService,
) {
    /**
     * 检查更新。
     *
     * Header 约定：
     * - `X-App-Code`: 应用编码
     * - `X-Platform`: 平台
     * - `X-Current-Version`: 当前版本（`x.y.z`）
     * - `X-Tenant-Id`: 租户 id
     * - `X-Device-Id`: 设备 id（可选）
     */
    @GetMapping("/check")
    fun check(
        @RequestHeader("X-App-Code") appCode: String,
        @RequestHeader("X-Platform") platform: String,
        @RequestHeader("X-Current-Version") currentVersion: String,
        @RequestHeader("X-Tenant-Id") tenantId: String,
        @RequestHeader(name = "X-Device-Id", required = false) deviceId: String?,
    ): ClientUpdateCheckResponse =
        clientUpdateService.check(
            appCode = appCode,
            platform = platform,
            currentVersion = currentVersion,
            tenantId = tenantId,
            deviceId = deviceId,
        )
}

