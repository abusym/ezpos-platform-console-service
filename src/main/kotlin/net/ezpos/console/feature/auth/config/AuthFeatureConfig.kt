package net.ezpos.console.feature.auth.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpaqueTokenProperties::class)
class AuthFeatureConfig

