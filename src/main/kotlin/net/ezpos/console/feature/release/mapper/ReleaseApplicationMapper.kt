package net.ezpos.console.feature.release.mapper

import net.ezpos.console.feature.release.dto.ReleaseApplicationDto
import net.ezpos.console.feature.release.entity.ReleaseApplication
import org.mapstruct.Mapper

/**
 * [ReleaseApplication] -> [ReleaseApplicationDto] 的对象映射器（MapStruct）。
 */
@Mapper(componentModel = "spring")
interface ReleaseApplicationMapper {
    /**
     * 将应用实体映射为对外 DTO。
     *
     * @param id 业务层决定暴露的 id（通常来自实体主键）
     * @param app 应用实体
     */
    fun toDto(id: Long, app: ReleaseApplication): ReleaseApplicationDto
}

