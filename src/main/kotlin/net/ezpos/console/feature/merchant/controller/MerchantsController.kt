package net.ezpos.console.feature.merchant.controller

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

@RestController
@RequestMapping("/api/merchants")
class MerchantsController(
    private val merchantService: MerchantService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateMerchantRequest): MerchantDto =
        merchantService.create(request)

    @GetMapping
    fun list(pageable: Pageable): Page<MerchantDto> =
        merchantService.list(pageable)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): MerchantDto =
        merchantService.getById(id)

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateMerchantRequest): MerchantDto =
        merchantService.update(id, request)

    @PostMapping("/{id}:enable")
    fun enable(@PathVariable id: Long): MerchantDto =
        merchantService.enable(id)

    @PostMapping("/{id}:disable")
    fun disable(@PathVariable id: Long): MerchantDto =
        merchantService.disable(id)
}
