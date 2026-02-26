package net.ezpos.console.feature.subscription.controller

import jakarta.validation.Valid
import net.ezpos.console.feature.subscription.dto.CreatePlanRequest
import net.ezpos.console.feature.subscription.dto.PlanDto
import net.ezpos.console.feature.subscription.dto.UpdatePlanRequest
import net.ezpos.console.feature.subscription.service.PlanService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plans")
class PlansController(
    private val planService: PlanService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlanRequest): PlanDto =
        planService.create(request)

    @GetMapping
    fun list(): List<PlanDto> =
        planService.list()

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdatePlanRequest): PlanDto =
        planService.update(id, request)
}
