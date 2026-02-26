package net.ezpos.console.feature.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ezpos.security.opaque-token")
data class OpaqueTokenProperties(
    val accessTokenTtl: Duration = Duration.ofHours(24),
    val redisKeyPrefix: String = "ezpos:platform:opaque-token:",
)

