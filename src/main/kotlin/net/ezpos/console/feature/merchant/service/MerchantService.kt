package net.ezpos.console.feature.merchant.service

import net.ezpos.console.common.exception.EntityNotFoundException
import net.ezpos.console.feature.merchant.dto.CreateMerchantRequest
import net.ezpos.console.feature.merchant.dto.MerchantDto
import net.ezpos.console.feature.merchant.dto.UpdateMerchantRequest
import net.ezpos.console.feature.merchant.entity.Merchant
import net.ezpos.console.feature.merchant.mapper.MerchantMapper
import net.ezpos.console.feature.merchant.repository.MerchantRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class MerchantService(
    private val repo: MerchantRepository,
    private val mapper: MerchantMapper,
) {
    fun create(request: CreateMerchantRequest): MerchantDto {
        val merchant = Merchant(
            name = request.name,
            contactName = request.contactName,
            contactPhone = request.contactPhone,
            address = request.address,
            memo = request.memo,
        )

        val saved = repo.save(merchant)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun list(pageable: Pageable): Page<MerchantDto> =
        repo.findAll(pageable).map { mapper.toDto(requireNotNull(it.id), it) }

    fun getById(id: Long): MerchantDto {
        val merchant = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }
        return mapper.toDto(requireNotNull(merchant.id), merchant)
    }

    fun update(id: Long, request: UpdateMerchantRequest): MerchantDto {
        val merchant = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }

        request.name?.let { merchant.name = it }
        request.contactName?.let { merchant.contactName = it }
        request.contactPhone?.let { merchant.contactPhone = it }
        request.address?.let { merchant.address = it }
        request.memo?.let { merchant.memo = it }

        val saved = repo.save(merchant)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun enable(id: Long): MerchantDto {
        val merchant = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }
        merchant.enabled = true
        val saved = repo.save(merchant)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }

    fun disable(id: Long): MerchantDto {
        val merchant = repo.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }
        merchant.enabled = false
        val saved = repo.save(merchant)
        return mapper.toDto(requireNotNull(saved.id), saved)
    }
}
