package net.ezpos.console.common.security.config

import net.ezpos.console.common.security.filter.OpaqueTokenAuthenticationFilter
import net.ezpos.console.common.security.spi.PrincipalLoader
import net.ezpos.console.common.security.spi.TokenIntrospector
import net.ezpos.console.common.web.problem.ProblemDetailAccessDeniedHandler
import net.ezpos.console.common.web.problem.ProblemDetailAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security 的统一配置入口。
 *
 * ## 关键策略
 * - **无状态（Stateless）**：不使用 HttpSession，所有请求依赖 token（适合 API 服务）。
 * - **CSRF 关闭**：在纯 API + token 场景下通常关闭（若引入浏览器 cookie 登录需重新评估）。
 * - **放行端点**：登录与 Swagger/OpenAPI 相关端点无需认证。
 * - **自定义认证 Filter**：在 `UsernamePasswordAuthenticationFilter` 之前插入
 *   [OpaqueTokenAuthenticationFilter] 完成 token -> principal 的装配。
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {
    /**
     * 密码编码器。
     *
     * 当前平台用户密码使用 BCrypt hash；后续若引入更复杂的策略（pepper/argon2），可在此处替换。
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * 构建安全过滤器链。
     *
     * @param tokenIntrospector token 解析实现（例如 opaque token -> userId）
     * @param principalLoader principal 加载实现（userId -> principal）
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        tokenIntrospector: TokenIntrospector,
        principalLoader: PrincipalLoader,
        authenticationEntryPoint: ProblemDetailAuthenticationEntryPoint,
        accessDeniedHandler: ProblemDetailAccessDeniedHandler,
    ): SecurityFilterChain {
        val tokenFilter = OpaqueTokenAuthenticationFilter(tokenIntrospector, principalLoader, authenticationEntryPoint)

        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .requestCache { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers(
                    "/api/auth/login",
                    "/api/client-updates/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

