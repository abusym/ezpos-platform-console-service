package net.ezpos.console.feature.subscription.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import net.ezpos.console.feature.subscription.dto.CreateSubscriptionRequest
import net.ezpos.console.feature.subscription.dto.RenewSubscriptionRequest
import net.ezpos.console.feature.subscription.dto.SubscriptionDto
import net.ezpos.console.feature.subscription.service.SubscriptionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "订阅管理", description = "订阅的创建、续费与查询")
@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionsController(
    private val subscriptionService: SubscriptionService,
) {
    @Operation(summary = "分页查询订阅")
    @GetMapping
    fun list(pageable: Pageable): Page<SubscriptionDto> =
        subscriptionService.list(pageable)

    @Operation(summary = "创建订阅")
    @PostMapping
    fun create(@Valid @RequestBody request: CreateSubscriptionRequest): SubscriptionDto =
        subscriptionService.create(request)

    @Operation(summary = "续费订阅")
    @PostMapping("/{id}:renew")
    fun renew(@PathVariable id: Long, @Valid @RequestBody request: RenewSubscriptionRequest): SubscriptionDto =
        subscriptionService.renew(id, request)

    @Operation(summary = "查询即将到期的订阅")
    @GetMapping("/expiring")
    fun listExpiring(@RequestParam(defaultValue = "30") days: Int): List<SubscriptionDto> =
        subscriptionService.listExpiring(days)
}
