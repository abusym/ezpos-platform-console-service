package net.ezpos.console.feature.release.mapper

import net.ezpos.console.feature.release.dto.ReleaseDto
import net.ezpos.console.feature.release.entity.Release
import net.ezpos.console.feature.release.service.WhitelistTenantsCodec
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * [Release] <-> [ReleaseDto] 的对象映射器（MapStruct）。
 *
 * 说明：
 * - DTO 的 `isForced` 字段命名与实体 `isForced`/`forced` 的 Java Bean 访问器存在差异，因此显式指定映射。
 * - 白名单租户在实体中以逗号分隔字符串存储，由业务层解析为 [List] 后再传入 DTO。
 */
@Mapper(componentModel = "spring")
interface ReleaseMapper {
    /**
     * 将发布实体与解析后的白名单租户转换为对外 DTO。
     *
     * @param id 业务层决定暴露的 id（通常来自实体主键）
     * @param release 发布实体
     * @param whitelistTenants 已解析的白名单租户列表（若无则传空列表）
     */
    @Mapping(target = "isForced", source = "release.forced")
    @Mapping(target = "whitelistTenants", source = "whitelistTenants")
    fun toDto(id: Long, release: Release, whitelistTenants: List<String>): ReleaseDto

    fun toDto(release: Release): ReleaseDto =
        toDto(
            id = requireNotNull(release.id),
            release = release,
            whitelistTenants = WhitelistTenantsCodec.decode(release.whitelistTenants),
        )
}

