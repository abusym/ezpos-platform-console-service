package net.ezpos.console.feature.subscription.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class SubscriptionStatus(
    @get:JsonValue val value: String,
) {
    ACTIVE("active"),
    EXPIRED("expired"),
    CANCELLED("cancelled"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): SubscriptionStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
    }
}
