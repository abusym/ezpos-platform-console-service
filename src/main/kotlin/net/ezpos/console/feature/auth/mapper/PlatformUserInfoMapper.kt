package net.ezpos.console.feature.auth.mapper

import net.ezpos.console.common.security.model.PlatformUserPrincipal
import net.ezpos.console.feature.auth.dto.PlatformUserInfo
import net.ezpos.console.feature.user.entity.PlatformUser
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface PlatformUserInfoMapper {
    fun fromUser(id: Long, user: PlatformUser): PlatformUserInfo

    @Mapping(target = "id", source = "principal.id")
    @Mapping(target = "username", source = "principal.username")
    @Mapping(target = "enabled", source = "principal.enabled")
    fun fromPrincipalAndUser(principal: PlatformUserPrincipal, user: PlatformUser?): PlatformUserInfo
}

