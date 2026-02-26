package net.ezpos.console.feature.merchant.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import net.ezpos.console.feature.merchant.dto.CreateMerchantRequest
import net.ezpos.console.feature.merchant.dto.MerchantDto
import net.ezpos.console.feature.merchant.dto.UpdateMerchantRequest
import net.ezpos.console.feature.merchant.service.MerchantService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "商家管理", description = "商家的增删改查与启停用")
@RestController
@RequestMapping("/api/merchants")
class MerchantsController(
    private val merchantService: MerchantService,
) {
    @Operation(summary = "创建商家")
    @PostMapping
    fun create(@Valid @RequestBody request: CreateMerchantRequest): MerchantDto =
        merchantService.create(request)

    @Operation(summary = "分页查询商家")
    @GetMapping
    fun list(pageable: Pageable): Page<MerchantDto> =
        merchantService.list(pageable)

    @Operation(summary = "获取商家详情")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): MerchantDto =
        merchantService.getById(id)

    @Operation(summary = "更新商家信息")
    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateMerchantRequest): MerchantDto =
        merchantService.update(id, request)

    @Operation(summary = "启用商家")
    @PostMapping("/{id}:enable")
    fun enable(@PathVariable id: Long): MerchantDto =
        merchantService.enable(id)

    @Operation(summary = "停用商家")
    @PostMapping("/{id}:disable")
    fun disable(@PathVariable id: Long): MerchantDto =
        merchantService.disable(id)
}
