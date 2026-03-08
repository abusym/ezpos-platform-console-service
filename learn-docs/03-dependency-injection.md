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

## 接口与实现

Spring DI 最强大的一点是**面向接口编程**。本项目中有个很好的例子：

```kotlin
// common/security/spi/ 定义接口
interface TokenIntrospector {
    fun resolveUserId(token: String): Long?
}

interface PrincipalLoader {
    fun load(userId: Long): PlatformUserPrincipal?
}
```

```kotlin
// feature/auth/token/ 提供实现
@Component
class OpaqueTokenService(...) : TokenIntrospector {
    override fun resolveUserId(token: String): Long? {
        // 从 Redis 查询 Token
    }
}
```

```kotlin
// feature/user/security/ 提供实现
@Component
class UserPrincipalLoader(...) : PrincipalLoader {
    override fun load(userId: Long): PlatformUserPrincipal? {
        // 从数据库查询用户
    }
}
```

```kotlin
// common/security/filter/ 使用接口
class OpaqueTokenAuthenticationFilter(
    private val tokenIntrospector: TokenIntrospector,  // 不关心具体实现
    private val principalLoader: PrincipalLoader       // 不关心具体实现
) {
    // 只要能 resolveUserId 和 load 就行
}
```

**好处**：`OpaqueTokenAuthenticationFilter` 完全不知道 Token 存在 Redis 还是数据库里。如果将来要换成 JWT，只需要写一个新的 `TokenIntrospector` 实现，Filter 代码零修改。

这和 Go 的接口是一样的思想：

```go
// Go 也是接口隔离
type TokenIntrospector interface {
    ResolveUserID(token string) (int64, error)
}

type OpaqueTokenService struct { redis *redis.Client }
func (s *OpaqueTokenService) ResolveUserID(token string) (int64, error) { ... }
```

区别在于 Go 的接口是**隐式实现**的（不需要声明 `implements`），Kotlin 需要**显式声明**（`: TokenIntrospector`）。
