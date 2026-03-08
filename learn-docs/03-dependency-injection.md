# 03 — 依赖注入（DI）详解

## 什么是依赖注入？

**依赖注入 = 别人帮你创建对象并传给你，而不是你自己 new。**

### 没有 DI 的 Go 代码

```go
// 你得自己创建依赖，从底层往上构建
func main() {
    db := gorm.Open(postgres.Open(dsn))
    userRepo := repository.NewUserRepo(db)          // 手动创建
    passwordEncoder := bcrypt.NewEncoder()            // 手动创建
    userService := service.NewUserService(userRepo, passwordEncoder) // 手动传入
    userHandler := handler.NewUserHandler(userService)  // 手动传入
}
```

### 有 DI 的 Spring 代码

```kotlin
// 你只需要声明"我需要什么"，Spring 自动给你
@Service
class PlatformUserService(
    private val repository: PlatformUserRepository,  // Spring 自动注入
    private val mapper: PlatformUserMapper,           // Spring 自动注入
    private val passwordEncoder: PasswordEncoder      // Spring 自动注入
) {
    // 直接用就行，不用关心它们是怎么创建的
}
```

**这里的 Kotlin 构造函数参数 = Go 的 struct 字段。** Spring 看到 `PlatformUserService` 的构造函数需要三个参数，就会从容器里找到对应类型的 Bean，自动传进去。

## 类比：餐厅厨师

想象你是一个厨师（Service），你需要食材（依赖）来做菜：

**没有 DI（Go 方式）：**
> "我得自己去菜市场买菜，自己找肉贩买肉，自己去渔港买海鲜。"

**有 DI（Spring 方式）：**
> "我只需要在菜单上写'需要：鸡蛋、面粉、牛奶'，食材自动出现在我的工作台上。"

Spring 容器 = 那个帮你采购食材的管理系统。

## 在本项目中的实际例子

### 例子 1：AuthService

```kotlin
@Service
class AuthService(
    private val userRepository: PlatformUserRepository,  // 需要查数据库
    private val tokenService: OpaqueTokenService,         // 需要操作 Token
    private val passwordEncoder: PasswordEncoder,         // 需要校验密码
    private val mapper: PlatformUserInfoMapper,           // 需要转换 DTO
    private val principalProvider: CurrentPrincipalProvider // 需要获取当前用户
) {
    fun login(request: LoginRequest): LoginResponse {
        // 直接使用注入的依赖，就像它们天生就在这里
        val user = userRepository.findByUsername(request.username)
            ?: throw AuthenticationFailedException("用户名或密码错误")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthenticationFailedException("用户名或密码错误")
        }

        val token = tokenService.issueAccessToken(user.id!!)
        return LoginResponse(
            accessToken = token,
            expiresInSeconds = tokenService.ttlSeconds(),
            user = mapper.toDto(user)
        )
    }
}
```

如果用 Go 写同样的代码：

```go
type AuthService struct {
    userRepo        *repository.UserRepo
    tokenService    *token.Service
    passwordEncoder *bcrypt.Encoder
}

// 你需要在 main() 里手动 new 并传入
func NewAuthService(repo *repository.UserRepo, ts *token.Service, enc *bcrypt.Encoder) *AuthService {
    return &AuthService{userRepo: repo, tokenService: ts, passwordEncoder: enc}
}
```

Spring 帮你省掉了 `NewAuthService(...)` 这一步，以及 `main()` 里所有的组装代码。

### 例子 2：Controller 依赖 Service

```kotlin
@RestController
@RequestMapping("/api/platform-users")
class PlatformUsersController(
    private val service: PlatformUserService  // Spring 自动注入
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreatePlatformUserRequest): PlatformUserDto {
        return service.create(request)
    }
}
```

Controller 只需要声明"我需要一个 PlatformUserService"，Spring 在创建 Controller 时自动把 Service 传进来。

## 注册 Bean 的几种方式

### 方式 1：注解扫描（最常用）

给类打上 `@Component`（或其变体）注解，Spring 自动发现并注册：

```kotlin
@Service        // @Service 是 @Component 的别名，语义更明确
class UserService { ... }

@Repository     // @Repository 也是 @Component 的别名
interface UserRepository : JpaRepository<User, Long> { ... }

@RestController // @RestController = @Controller + @ResponseBody
class UserController { ... }

@Component      // 通用组件
class SnowflakeIdGenerator { ... }
```

### 方式 2：@Bean 方法（用于第三方库的对象）

当你需要注册一个不是你写的类（无法加注解）时，用 `@Bean`：

```kotlin
@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    // 这样容器里就有了一个 PasswordEncoder 类型的 Bean
    // 其他类需要 PasswordEncoder 时，Spring 就会注入这个实例
}
```

类比 Go：这相当于你在 `main()` 里 `encoder := bcrypt.New()`，但 Spring 帮你管理生命周期。

### 方式 3：配置属性绑定

```kotlin
@ConfigurationProperties(prefix = "ezpos.security.opaque-token")
data class OpaqueTokenProperties(
    val accessTokenTtl: Duration = Duration.ofHours(24),
    val redisKeyPrefix: String = "ezpos:platform:opaque-token:"
)
```

这会自动读取 `application.yaml` 里的配置并创建为 Bean：

```yaml
ezpos:
  security:
    opaque-token:
      access-token-ttl: 24h
      redis-key-prefix: "ezpos:platform:opaque-token:"
```

## Bean 的生命周期

默认情况下，Spring 的 Bean 是**单例**的（Singleton）——整个应用只有一个实例。

这和 Go 里你在 `main()` 创建一个 `userService` 然后到处传是一样的效果。区别是 Spring 帮你自动管理了创建、初始化和销毁。

```
应用启动 → 创建 Bean → 注入依赖 → Bean 就绪 → ... 使用中 ... → 应用关闭 → 销毁 Bean
```

## 接口与实现——SPI 模式详解

### 先搞清楚 SPI 到底是什么

**SPI = Service Provider Interface（服务提供者接口）。**

别被名字吓到，它的本质非常简单：

> **SPI = "我定义规则，你来实现"**

你一定在前端写过这样的代码：

```typescript
// 定义一个接口（规则）
interface StorageAdapter {
  get(key: string): string | null
  set(key: string, value: string): void
}

// 实现 1：用 localStorage
class LocalStorageAdapter implements StorageAdapter {
  get(key: string) { return localStorage.getItem(key) }
  set(key: string, value: string) { localStorage.setItem(key, value) }
}

// 实现 2：用 sessionStorage
class SessionStorageAdapter implements StorageAdapter {
  get(key: string) { return sessionStorage.getItem(key) }
  set(key: string, value: string) { sessionStorage.setItem(key, value) }
}

// 使用方只认接口，不关心具体实现
class AuthManager {
  constructor(private storage: StorageAdapter) {}
  saveToken(token: string) { this.storage.set('token', token) }
}
```

**SPI 就是这个 `StorageAdapter` 接口**——它定义了"能力规格"，具体实现可以随时替换。

在 Go 里你也一定用过：

```go
// Go 的接口天然就是 SPI
type Logger interface {
    Info(msg string)
    Error(msg string)
}

// 实现 1: 打印到控制台
type ConsoleLogger struct{}
func (l *ConsoleLogger) Info(msg string) { fmt.Println("[INFO]", msg) }

// 实现 2: 写到文件
type FileLogger struct{ file *os.File }
func (l *FileLogger) Info(msg string) { l.file.WriteString("[INFO] " + msg) }
```

Go 里你天天在用 SPI 模式，只是没人这么叫它。**在 Spring 生态里，把这种"接口定义在公共层，实现提供在业务层"的模式显式地叫做 SPI。**

### 为什么需要 SPI？——一个依赖方向的问题

假设没有 SPI，代码会变成这样：

```
common/security/filter/
    OpaqueTokenAuthenticationFilter
        → 直接 import feature/auth/token/OpaqueTokenService   ← 公共模块依赖业务模块！
        → 直接 import feature/user/security/UserPrincipalLoader ← 公共模块依赖业务模块！
```

**这就像前端的组件库 (antd) 去 import 你的业务代码一样荒谬。** 公共模块应该是被依赖的，不应该反过来依赖业务模块。

SPI 的作用就是**反转依赖方向**：

```
common/security/spi/                     ← 公共层只定义接口
    TokenIntrospector (interface)
    PrincipalLoader (interface)

common/security/filter/                  ← 公共层只依赖接口
    OpaqueTokenAuthenticationFilter
        → 依赖 TokenIntrospector          ← 依赖接口，不依赖实现
        → 依赖 PrincipalLoader            ← 依赖接口，不依赖实现

feature/auth/token/                      ← 业务层提供实现
    OpaqueTokenService : TokenIntrospector
        → 依赖 common/security/spi        ← 业务模块依赖公共模块（方向正确！）

feature/user/security/                   ← 业务层提供实现
    UserPrincipalLoader : PrincipalLoader
        → 依赖 common/security/spi        ← 业务模块依赖公共模块（方向正确！）
```

画成图更清楚：

```
        ┌─────────────────────────────────────────────────┐
        │               common/security                    │
        │                                                  │
        │   ┌────────────────────┐  ┌──────────────────┐  │
        │   │  TokenIntrospector │  │  PrincipalLoader  │  │
        │   │    (interface)     │  │    (interface)     │  │
        │   └────────▲───────────┘  └────────▲──────────┘  │
        │            │                       │             │
        │   ┌────────┴───────────────────────┴──────────┐  │
        │   │   OpaqueTokenAuthenticationFilter         │  │
        │   │   "我只认接口，不管谁实现"                   │  │
        │   └───────────────────────────────────────────┘  │
        └─────────────────────────────────────────────────┘
                     ▲                       ▲
                     │ implements             │ implements
                     │                       │
        ┌────────────┴────────┐  ┌───────────┴────────────┐
        │  feature/auth       │  │  feature/user           │
        │                     │  │                         │
        │  OpaqueTokenService │  │  UserPrincipalLoader    │
        │  (从 Redis 查 Token) │  │  (从数据库查用户)        │
        └─────────────────────┘  └─────────────────────────┘
```

### 本项目中的 SPI 实战

#### 第一步：common 层定义接口（SPI 契约）

```kotlin
// 文件：common/security/spi/TokenIntrospector.kt
// 作用：定义"Token 怎么验证"的规格，不关心具体实现

interface TokenIntrospector {
    /**
     * 给我一个 token 字符串，告诉我它对应哪个用户。
     * 返回 null 表示 token 无效。
     */
    fun resolveUserId(token: String): Long?
}
```

```kotlin
// 文件：common/security/spi/PrincipalLoader.kt
// 作用：定义"怎么加载用户信息"的规格

interface PrincipalLoader {
    /**
     * 给我一个 userId，把用户信息加载出来。
     * 返回 null 表示用户不存在或不可用。
     */
    fun load(userId: Long): PlatformUserPrincipal?
}
```

#### 第二步：common 层的 Filter 只依赖接口

```kotlin
// 文件：common/security/filter/OpaqueTokenAuthenticationFilter.kt
// 注意构造函数：只要求接口类型，不关心谁实现

class OpaqueTokenAuthenticationFilter(
    private val tokenIntrospector: TokenIntrospector,  // 接口！
    private val principalLoader: PrincipalLoader,       // 接口！
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: ..., response: ..., filterChain: ...) {
        // 1. 提取 Bearer Token
        val header = request.getHeader("Authorization")
        val token = header.substring(7).trim()

        // 2. 调用 SPI：Token → userId（不知道也不关心 Token 存在哪）
        val userId = tokenIntrospector.resolveUserId(token) ?: run {
            // token 无效，返回 401
            authenticationEntryPoint.commence(request, response, ...)
            return
        }

        // 3. 调用 SPI：userId → 用户信息（不知道也不关心用户存在哪个数据库）
        val principal = principalLoader.load(userId) ?: run {
            // 用户不存在，返回 401
            authenticationEntryPoint.commence(request, response, ...)
            return
        }

        // 4. 认证成功，设置安全上下文
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
        filterChain.doFilter(request, response)
    }
}
```

#### 第三步：业务模块提供实现

```kotlin
// 文件：feature/auth/token/OpaqueTokenService.kt
// 这是 TokenIntrospector 的具体实现：用 Redis 存储 Token

@Service  // 注册为 Spring Bean，Spring 自动把它注入到需要 TokenIntrospector 的地方
class OpaqueTokenService(
    private val redis: StringRedisTemplate,
    private val props: OpaqueTokenProperties,
) : TokenIntrospector {  // ← 显式声明"我实现了 TokenIntrospector 接口"

    // 生成 Token 并存入 Redis
    fun issueAccessToken(userId: Long): String {
        val token = generateToken()
        redis.opsForValue().set(key(token), userId.toString(), props.accessTokenTtl)
        return token
    }

    // 实现接口方法：从 Redis 查询 Token 对应的 userId
    override fun resolveUserId(token: String): Long? {
        val raw = redis.opsForValue().get(key(token)) ?: return null
        val userId = raw.toLongOrNull() ?: return null
        redis.expire(key(token), props.accessTokenTtl)  // 滑动续期
        return userId
    }

    fun revoke(token: String) { redis.delete(key(token)) }

    private fun key(token: String): String = props.redisKeyPrefix + token
}
```

```kotlin
// 文件：feature/user/security/UserPrincipalLoader.kt
// 这是 PrincipalLoader 的具体实现：从数据库加载用户

@Service
class UserPrincipalLoader(
    private val repo: PlatformUserRepository,
) : PrincipalLoader {  // ← 显式声明"我实现了 PrincipalLoader 接口"

    override fun load(userId: Long): PlatformUserPrincipal? {
        val user = repo.findById(userId).orElse(null) ?: return null
        if (!user.enabled) return null  // 被禁用的用户视为不存在

        return PlatformUserPrincipal(
            id = userId,
            passwordHash = user.passwordHash,
            enabled = user.enabled,
            usernameValue = user.username,
        )
    }
}
```

#### 第四步：Spring 自动"接线"

当应用启动时，Spring 容器做了以下事情：

```
1. 扫描到 OpaqueTokenService 标注了 @Service，创建它的实例
2. 发现 OpaqueTokenService 实现了 TokenIntrospector 接口
   → 记录：TokenIntrospector 的实现是 OpaqueTokenService

3. 扫描到 UserPrincipalLoader 标注了 @Service，创建它的实例
4. 发现 UserPrincipalLoader 实现了 PrincipalLoader 接口
   → 记录：PrincipalLoader 的实现是 UserPrincipalLoader

5. 创建 OpaqueTokenAuthenticationFilter 时需要：
   - TokenIntrospector → 找到 OpaqueTokenService，注入！
   - PrincipalLoader   → 找到 UserPrincipalLoader，注入！
```

你什么都不用手动组装，Spring 根据类型自动匹配接口和实现。

### 用 Go 类比完整流程

如果用 Go + Gin 实现同样的 SPI 模式：

```go
// ===== 公共层：定义接口 =====

// spi/token_introspector.go
type TokenIntrospector interface {
    ResolveUserID(token string) (int64, error)
}

// spi/principal_loader.go
type PrincipalLoader interface {
    Load(userID int64) (*Principal, error)
}

// middleware/auth.go — 只依赖接口
func AuthMiddleware(ti TokenIntrospector, pl PrincipalLoader) gin.HandlerFunc {
    return func(c *gin.Context) {
        token := extractBearer(c)
        userID, err := ti.ResolveUserID(token)  // 调用接口
        if err != nil { c.AbortWithStatus(401); return }
        principal, err := pl.Load(userID)       // 调用接口
        if err != nil { c.AbortWithStatus(401); return }
        c.Set("principal", principal)
        c.Next()
    }
}

// ===== 业务层：提供实现 =====

// auth/opaque_token_service.go — 实现 TokenIntrospector
type OpaqueTokenService struct { redis *redis.Client }
func (s *OpaqueTokenService) ResolveUserID(token string) (int64, error) {
    val, err := s.redis.Get(ctx, "token:"+token).Result()
    // ...
}

// user/user_principal_loader.go — 实现 PrincipalLoader
type UserPrincipalLoader struct { db *gorm.DB }
func (l *UserPrincipalLoader) Load(userID int64) (*Principal, error) {
    var user User
    l.db.First(&user, userID)
    // ...
}

// ===== main.go：手动组装（Spring 自动完成的部分） =====
func main() {
    tokenService := &OpaqueTokenService{redis: redisClient}
    principalLoader := &UserPrincipalLoader{db: db}

    r := gin.Default()
    r.Use(AuthMiddleware(tokenService, principalLoader))  // 手动注入
    r.Run(":8080")
}
```

**在 Go 里，`main()` 中的手动组装是必须的。在 Spring 里，这一步完全由容器自动完成。**

两者的核心思想完全一样：

| | Go | Kotlin/Spring |
|---|---|---|
| 定义接口 | `type Interface interface {}` | `interface Interface {}` |
| 实现接口 | 隐式（只要方法签名匹配就行） | 显式（`: Interface`） |
| 接口发现 | 编译器自动推断 | Spring 容器扫描注解 |
| 组装（接线） | 手动在 `main()` 里组装 | Spring 容器自动注入 |

### SPI 模式的实际价值

**场景：将来产品说"Token 要改成 JWT"**

没有 SPI 的情况——你得改：
- `OpaqueTokenAuthenticationFilter.kt`（改 Token 解析逻辑）
- 所有涉及 Token 操作的代码

有 SPI 的情况——你只需要：
1. 新建一个 `JwtTokenService`，实现 `TokenIntrospector` 接口
2. 把 `OpaqueTokenService` 上的 `@Service` 注解去掉（或用 `@Primary` 标记新实现）
3. 完事。Filter、Controller、其他 Service 一行代码都不用改。

```kotlin
// 新实现：JWT 方式
@Service  // 替换原来的 OpaqueTokenService
class JwtTokenService(private val jwtSecret: String) : TokenIntrospector {
    override fun resolveUserId(token: String): Long? {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .body.subject.toLongOrNull()
    }
}
```

Spring 容器会自动发现新的 `TokenIntrospector` 实现，把它注入到 Filter 中。整个替换过程不涉及 common 模块的任何修改。

**这就是 SPI 的价值：让"变化"被隔离在一个小范围内，而不是在整个项目中到处改。**
