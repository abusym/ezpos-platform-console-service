package net.ezpos.console.feature.subscription.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "订阅套餐", description = "套餐的创建、查询与更新")
@RestController
@RequestMapping("/api/plans")
class PlansController(
    private val planService: PlanService,
) {
    @Operation(summary = "创建套餐")
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlanRequest): PlanDto =
        planService.create(request)

    @Operation(summary = "查询所有套餐")
    @GetMapping
    fun list(): List<PlanDto> =
        planService.list()

    @Operation(summary = "更新套餐")
    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdatePlanRequest): PlanDto =
        planService.update(id, request)
}
