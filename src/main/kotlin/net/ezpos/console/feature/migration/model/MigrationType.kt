package net.ezpos.console.feature.migration.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class MigrationType(
    @get:JsonValue val value: String,
) {
    PRODUCT("product"),
    CATEGORY("category"),
    MEMBER("member"),
    FULL("full"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): MigrationType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown migration type: $value")
    }
}
