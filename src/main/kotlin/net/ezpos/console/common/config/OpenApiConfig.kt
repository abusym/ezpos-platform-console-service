package net.ezpos.console.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("EzPos 平台总后台 API")
                .description("EzPos SaaS 平台管理后台接口文档，包含认证、用户管理、商家管理、订阅套餐、版本发布、数据迁移、审计日志等模块。")
                .version("1.0.0")
                .contact(Contact().name("EzPos Team")),
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "bearer-token",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("不透明令牌（Opaque Token），通过登录接口获取"),
                ),
        )
        .addSecurityItem(SecurityRequirement().addList("bearer-token"))
}
