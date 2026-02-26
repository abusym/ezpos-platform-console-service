package net.ezpos.console.feature.release.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 发布灰度/投放策略类型。
 *
 * 该枚举通过 `value` 字段与 JSON 进行映射（大小写不敏感），用于 API 传输时保持前后端一致的字符串取值。
 */
enum class ReleaseRolloutType(
    @get:JsonValue val value: String,
) {
    /** 全量：满足其他条件的所有租户/客户端均命中该版本。 */
    ALL("all"),

    /** 百分比：按租户/客户端在稳定散列空间中的分桶结果决定是否命中。 */
    PERCENT("percent"),

    /** 白名单：仅白名单中的租户命中该版本。 */
    WHITELIST("whitelist"),
    ;

    companion object {
        /**
         * 从 JSON 字符串反序列化为 [ReleaseRolloutType]（大小写不敏感）。
         *
         * @param value JSON 中的策略字符串
         * @throws IllegalArgumentException 当传入值无法匹配任何枚举项时抛出
         */
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): ReleaseRolloutType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown rolloutType: $value")
    }
}

