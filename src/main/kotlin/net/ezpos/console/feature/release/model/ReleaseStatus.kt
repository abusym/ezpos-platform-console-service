package net.ezpos.console.feature.release.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 发布状态。
 *
 * 该枚举通过 `value` 字段与 JSON 进行映射（大小写不敏感），用于 API/持久化层之间的稳定传输。
 */
enum class ReleaseStatus(
    @get:JsonValue val value: String,
) {
    /** 已发布：客户端可被命中（是否命中仍可能受 rollout 规则影响）。 */
    PUBLISHED("published"),

    /** 暂停发布：即使存在发布记录也不应对客户端生效。 */
    PAUSED("paused"),
    ;

    companion object {
        /**
         * 从 JSON 字符串反序列化为 [ReleaseStatus]（大小写不敏感）。
         *
         * @param value JSON 中的状态字符串
         * @throws IllegalArgumentException 当传入值无法匹配任何枚举项时抛出
         */
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): ReleaseStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
    }
}

