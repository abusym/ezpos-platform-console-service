package net.ezpos.console.feature.migration.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class MigrationStatus(
    @get:JsonValue val value: String,
) {
    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): MigrationStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown migration status: $value")
    }
}
