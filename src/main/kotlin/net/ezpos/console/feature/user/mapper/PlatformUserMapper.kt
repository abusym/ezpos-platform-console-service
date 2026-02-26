package net.ezpos.console.feature.user.mapper

import net.ezpos.console.feature.user.dto.PlatformUserDto
import net.ezpos.console.feature.user.entity.PlatformUser
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface PlatformUserMapper {
    fun toDto(id: Long, user: PlatformUser): PlatformUserDto
}

