package net.ezpos.console.feature.subscription.controller

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

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionsController(
    private val subscriptionService: SubscriptionService,
) {
    @GetMapping
    fun list(pageable: Pageable): Page<SubscriptionDto> =
        subscriptionService.list(pageable)

    @PostMapping
    fun create(@Valid @RequestBody request: CreateSubscriptionRequest): SubscriptionDto =
        subscriptionService.create(request)

    @PostMapping("/{id}:renew")
    fun renew(@PathVariable id: Long, @Valid @RequestBody request: RenewSubscriptionRequest): SubscriptionDto =
        subscriptionService.renew(id, request)

    @GetMapping("/expiring")
    fun listExpiring(@RequestParam(defaultValue = "30") days: Int): List<SubscriptionDto> =
        subscriptionService.listExpiring(days)
}
