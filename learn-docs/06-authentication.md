# 06 — 认证系统详解

## 认证方案总览

本项目使用**不透明令牌（Opaque Token）+ Redis** 方案。这和你在 Gin 项目里用 Redis 存 session/token 的方式几乎一样。

### 认证流程图

```
客户端                              服务端                          Redis
  │                                  │                              │
  │  POST /api/auth/login            │                              │
  │  { username, password }          │                              │
  │ ───────────────────────────────→ │                              │
  │                                  │  校验用户名密码               │
  │                                  │  生成随机 Token               │
  │                                  │  SET token → userId          │
  │                                  │ ────────────────────────────→ │
  │                                  │             TTL: 24h          │
  │  { accessToken: "abc123..." }    │                              │
  │ ←─────────────────────────────── │                              │
  │                                  │                              │
  │  GET /api/merchants              │                              │
  │  Authorization: Bearer abc123    │                              │
  │ ───────────────────────────────→ │                              │
  │                                  │  GET token → userId          │
  │                                  │ ────────────────────────────→ │
  │                                  │         userId: 12345         │
  │                                  │  续期 TTL: 24h（滑动窗口）     │
  │                                  │ ────────────────────────────→ │
  │                                  │  查询用户信息                  │
  │                                  │  设置安全上下文                │
  │  { merchants: [...] }            │                              │
  │ ←─────────────────────────────── │                              │
```

## Gin 中间件 vs Spring Filter

### Gin 认证中间件

```go
func AuthMiddleware() gin.HandlerFunc {
    return func(c *gin.Context) {
        token := c.GetHeader("Authorization")
        if token == "" {
            c.JSON(401, gin.H{"error": "未授权"})
            c.Abort()
            return
        }
        token = strings.TrimPrefix(token, "Bearer ")

        // 从 Redis 查找 userId
        userId, err := redis.Get(ctx, "token:"+token).Result()
        if err != nil {
            c.JSON(401, gin.H{"error": "Token 无效"})
            c.Abort()
            return
        }

        // 查询用户信息
        var user User
        db.First(&user, userId)

        // 设置到上下文中
        c.Set("currentUser", &user)
        c.Next()
    }
}
```

### Spring Filter（本项目的实现）

```kotlin
class OpaqueTokenAuthenticationFilter(
    private val tokenIntrospector: TokenIntrospector,
    private val principalLoader: PrincipalLoader,
    private val problemDetailWriter: ProblemDetailWriter
) : OncePerRequestFilter() {

    // 哪些路径不需要认证
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/client-updates") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. 提取 Token
        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            writeProblem(response, 401, "缺少认证令牌")
            return
        }
        val token = header.removePrefix("Bearer ")

        // 2. 从 Redis 解析 userId（通过 TokenIntrospector SPI）
        val userId = tokenIntrospector.resolveUserId(token)
        if (userId == null) {
            writeProblem(response, 401, "令牌无效或已过期")
            return
        }

        // 3. 加载用户信息（通过 PrincipalLoader SPI）
        val principal = principalLoader.load(userId)
        if (principal == null || !principal.isEnabled) {
            writeProblem(response, 401, "用户不存在或已禁用")
            return
        }

        // 4. 设置安全上下文（相当于 c.Set("currentUser", user)）
        val auth = UsernamePasswordAuthenticationToken(
            principal, null, principal.authorities
        )
        SecurityContextHolder.getContext().authentication = auth

        // 5. 继续处理请求（相当于 c.Next()）
        filterChain.doFilter(request, response)
    }
}
```

**对比：**

| Gin | Spring |
|-----|--------|
| `c.GetHeader("Authorization")` | `request.getHeader("Authorization")` |
| `c.JSON(401, ...)` + `c.Abort()` | `writeProblem(response, 401, ...)` + `return` |
| `redis.Get(ctx, key)` | `tokenIntrospector.resolveUserId(token)` |
| `c.Set("currentUser", user)` | `SecurityContextHolder.getContext().authentication = auth` |
| `c.Next()` | `filterChain.doFilter(request, response)` |

逻辑完全一样！只是 Spring 用了更多的抽象层。

## Token 服务（OpaqueTokenService）

```kotlin
@Component
class OpaqueTokenService(
    private val redisTemplate: StringRedisTemplate,   // Redis 客户端
    private val properties: OpaqueTokenProperties      // 配置属性
) : TokenIntrospector {

    // 生成 Token 并存入 Redis
    fun issueAccessToken(userId: Long): String {
        val token = generateSecureToken()              // Base64 随机 32 字节
        val key = properties.redisKeyPrefix + token    // "ezpos:platform:opaque-token:abc123"
        redisTemplate.opsForValue()
            .set(key, userId.toString(), properties.accessTokenTtl)  // SET key userId EX 86400
        return token
    }

    // 验证 Token，返回 userId
    override fun resolveUserId(token: String): Long? {
        val key = properties.redisKeyPrefix + token
        val userId = redisTemplate.opsForValue().get(key) ?: return null
        // 滑动窗口续期
        redisTemplate.expire(key, properties.accessTokenTtl)
        return userId.toLong()
    }

    // 删除 Token（登出）
    fun revoke(token: String) {
        redisTemplate.delete(properties.redisKeyPrefix + token)
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
```

对比 Go 版本：

```go
func (s *TokenService) IssueToken(userId int64) string {
    token := generateRandomToken()
    key := "ezpos:platform:opaque-token:" + token
    s.redis.Set(ctx, key, strconv.FormatInt(userId, 10), 24*time.Hour)
    return token
}

func (s *TokenService) ResolveUserId(token string) (int64, error) {
    key := "ezpos:platform:opaque-token:" + token
    val, err := s.redis.Get(ctx, key).Result()
    if err != nil {
        return 0, err
    }
    s.redis.Expire(ctx, key, 24*time.Hour)  // 续期
    return strconv.ParseInt(val, 10, 64)
}
```

## 安全配置（SecurityConfig）

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        tokenFilter: OpaqueTokenAuthenticationFilter
    ): SecurityFilterChain {
        http {
            csrf { disable() }                         // 禁用 CSRF（纯 API 不需要）
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS  // 无 Session
            }
            authorizeHttpRequests {
                // 公开路径
                authorize("/api/auth/login", permitAll)
                authorize("/api/client-updates/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/actuator/**", permitAll)
                // 其他路径需要认证
                authorize(anyRequest, authenticated)
            }
            // 在 Spring Security 默认 Filter 之前插入我们的 Token Filter
            addFilterBefore<UsernamePasswordAuthenticationFilter>(tokenFilter)
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
```

对比 Gin 的路由中间件：

```go
// Gin: 手动给需要认证的路由组加中间件
public := r.Group("/api")
public.POST("/auth/login", authHandler.Login)      // 不加中间件 = 公开

private := r.Group("/api", authMiddleware())        // 加中间件 = 需要认证
private.GET("/merchants", merchantHandler.List)
```

Spring 是在一个地方统一配置，Gin 是在注册路由时决定。效果一样。

## 获取当前登录用户

### Gin 方式

```go
func (h *Handler) SomeEndpoint(c *gin.Context) {
    user, _ := c.Get("currentUser")
    currentUser := user.(*User)
    // 使用 currentUser
}
```

### Spring 方式（本项目）

```kotlin
// 接口定义
interface CurrentPrincipalProvider {
    fun current(): PlatformUserPrincipal
    fun currentOrNull(): PlatformUserPrincipal?
}

// 实现：从 Spring Security 上下文获取
@Component
class SpringSecurityCurrentPrincipalProvider : CurrentPrincipalProvider {
    override fun current(): PlatformUserPrincipal {
        return currentOrNull() ?: throw AuthenticationFailedException("未登录")
    }

    override fun currentOrNull(): PlatformUserPrincipal? {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.principal as? PlatformUserPrincipal
    }
}

// 在 Service 中使用
@Service
class AuthService(
    private val principalProvider: CurrentPrincipalProvider
) {
    fun me(): PlatformUserInfo {
        val principal = principalProvider.current()  // 获取当前用户
        // ...
    }
}
```

## 登录流程完整代码

```kotlin
@Service
class AuthService(
    private val userRepository: PlatformUserRepository,
    private val tokenService: OpaqueTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val mapper: PlatformUserInfoMapper,
    private val principalProvider: CurrentPrincipalProvider
) {
    fun login(request: LoginRequest): LoginResponse {
        // 1. 查找用户
        val user = userRepository.findByUsername(request.username)
            ?: throw AuthenticationFailedException("用户名或密码错误")

        // 2. 校验密码
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthenticationFailedException("用户名或密码错误")
        }

        // 3. 检查用户是否启用
        if (!user.enabled) {
            throw AuthenticationFailedException("账户已禁用")
        }

        // 4. 生成 Token
        val token = tokenService.issueAccessToken(user.id!!)

        // 5. 返回登录响应
        return LoginResponse(
            tokenType = "Bearer",
            accessToken = token,
            expiresInSeconds = tokenService.ttlSeconds(),
            user = mapper.toDto(user)
        )
    }
}
```

## 默认管理员初始化

```kotlin
@Component
class PlatformUserBootstrap(
    private val userRepository: PlatformUserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @EventListener(ApplicationReadyEvent::class)  // 应用启动完成后执行
    fun init() {
        if (userRepository.count() == 0L) {
            val admin = PlatformUser(
                username = "admin",
                passwordHash = passwordEncoder.encode("changeme"),
                displayName = "管理员",
                email = null,
                enabled = true
            )
            userRepository.save(admin)
        }
    }
}
```

这相当于你在 Go 里的 `init()` 函数或者 `main()` 里的初始化代码——如果数据库里没有用户，自动创建一个默认管理员。
