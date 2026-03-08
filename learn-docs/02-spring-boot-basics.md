# 02 — Spring Boot 核心概念

## 先理解 Kotlin

Kotlin 是 JetBrains 开发的语言，运行在 JVM（Java 虚拟机）上。你可以把它理解为：

> **Kotlin 之于 Java ≈ TypeScript 之于 JavaScript**

- 更简洁的语法
- 内置空安全（`?` 操作符，和 TypeScript 的 `?` 一样）
- 数据类（`data class`，类似 Go 的 struct 但自带 equals/hashCode/toString）
- 可以和 Java 库无缝互操作

### 快速 Kotlin 语法对照

```kotlin
// 变量
val name = "Zhang"          // 不可变（类似 const）
var age = 25                // 可变（类似 let）

// 函数
fun add(a: Int, b: Int): Int = a + b

// 类
class User(
    val name: String,       // 构造参数 + 属性 一步到位
    var age: Int = 0        // 默认值
)

// 空安全
val len: Int? = name?.length    // name 可能为 null
val len2: Int = name!!.length   // 断言非空（不推荐）

// 数据类（自动生成 equals/hashCode/toString/copy）
data class UserDto(val id: Long, val name: String)

// 字符串模板
val msg = "Hello, ${user.name}!"

// Lambda
val filtered = list.filter { it.age > 18 }

// when 表达式（类似 switch，但更强大）
when (status) {
    ACTIVE -> "活跃"
    EXPIRED -> "过期"
    else -> "未知"
}
```

## Spring Boot 是什么？

你用过 Gin，Gin 是一个**轻量级 Web 框架**——它只管路由和 HTTP 处理，其他一切你自己搭。

**Spring Boot = Gin + GORM + 配置管理 + 依赖注入 + 安全框架 + ...**

它是一个**全家桶框架**，帮你把几乎所有后端需要的东西都集成好了。

### 类比理解

| 概念 | Gin 生态 | Spring Boot |
|------|---------|-------------|
| 路由 | `gin.Default()` + `r.GET(...)` | `@RestController` + `@GetMapping` |
| 中间件 | `r.Use(AuthMiddleware)` | `Filter` / `@Component` |
| 数据库 | GORM（手动初始化） | JPA/Hibernate（自动配置） |
| 配置 | viper / godotenv | `application.yaml`（内置） |
| 依赖注入 | 手动 / wire | Spring IoC（全自动） |
| 参数校验 | go-playground/validator | Jakarta Validation（`@NotBlank` 等） |
| API 文档 | swaggo | springdoc-openapi / Swagger |

## 核心注解速查

**注解（Annotation）就是"给代码贴标签"**。Spring 在启动时扫描这些标签，决定怎么处理这个类/方法/字段。

### 类级别注解

```kotlin
@RestController         // "我是一个 API 控制器，所有方法的返回值自动转 JSON"
@RequestMapping("/api") // "我的所有路由都以 /api 开头"
@Service                // "我是一个业务逻辑类，请把我注册到容器里"
@Repository             // "我是一个数据库操作类"
@Component              // "我是一个通用组件，请把我注册到容器里"
@Configuration          // "我是一个配置类，里面有一些 Bean 定义"
@Entity                 // "我对应数据库里的一张表"
```

### 方法级别注解

```kotlin
@GetMapping("/users")       // 处理 GET /users
@PostMapping("/users")      // 处理 POST /users
@PatchMapping("/users/{id}") // 处理 PATCH /users/:id
@DeleteMapping("/users/{id}") // 处理 DELETE /users/:id
@Transactional              // "这个方法需要数据库事务"
```

### 参数级别注解

```kotlin
fun create(
    @RequestBody request: CreateUserRequest,  // 从请求 body 解析 JSON
    @PathVariable id: Long,                   // 从 URL 路径取参数（:id）
    @RequestParam page: Int,                  // 从 query string 取参数（?page=1）
    @RequestHeader("X-Token") token: String   // 从请求头取值
)
```

### 对比 Gin 的写法

```go
// Gin
func (h *UserHandler) Create(c *gin.Context) {
    var req CreateUserRequest
    if err := c.ShouldBindJSON(&req); err != nil {  // 手动绑定 body
        c.JSON(400, gin.H{"error": err.Error()})
        return
    }
    id := c.Param("id")           // 手动取路径参数
    page := c.Query("page")       // 手动取查询参数
    token := c.GetHeader("X-Token") // 手动取请求头
    // ...
}
```

```kotlin
// Spring Boot — 注解声明，框架自动绑定
@PostMapping("/{id}")
fun create(
    @RequestBody request: CreateUserRequest,  // 自动绑定 body
    @PathVariable id: Long,                   // 自动取路径参数
    @RequestParam page: Int,                  // 自动取查询参数
    @RequestHeader("X-Token") token: String   // 自动取请求头
): UserDto {
    return userService.create(request)
    // 返回值自动转 JSON，不需要 c.JSON()
}
```

## Bean 是什么？

**Bean = Spring 容器管理的对象实例。**

在 Go 里，你需要手动创建对象：

```go
db := connectDB()
userRepo := NewUserRepo(db)
userService := NewUserService(userRepo)
handler := NewUserHandler(userService)
```

在 Spring 里，你只需要给类打上注解（`@Service`、`@Repository`、`@Component`、`@RestController`），Spring 容器在启动时会：

1. 扫描所有带这些注解的类
2. 自动创建它们的实例（这些实例就叫 **Bean**）
3. 自动把依赖注入进去（比如 Service 需要 Repository，Spring 会自动传进去）

你可以把 Spring 容器想象成一个**超级工厂**：

```
你告诉工厂："我需要一个 UserService"（通过打 @Service 注解）
工厂说："好的，UserService 需要 UserRepository，让我先造一个 UserRepository"
工厂说："UserRepository 需要数据库连接，让我先配置好数据库"
工厂说："全部造好了，给你！"
```

## 配置文件：application.yaml

Spring Boot 用 YAML 配置文件，和你用 Gin 时的配置文件概念一样，但它有一些特殊能力：

### Profile（环境配置）

```yaml
# application.yaml — 默认配置（开发环境）
spring:
  profiles:
    active: dev

# application-prod.yaml — 生产环境配置（通过环境变量 SPRING_PROFILES_ACTIVE=prod 激活）
spring:
  datasource:
    url: ${DB_URL}           # 引用环境变量
```

这相当于你 Go 项目里用 `APP_ENV=production` 来切换配置。

### 自动配置的魔力

当你在 `application.yaml` 里写了数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ezpos
```

Spring Boot 会**自动**：
1. 创建数据库连接池
2. 配置 JPA/Hibernate
3. 初始化事务管理器
4. 运行 Flyway 迁移

你不需要在代码里写任何初始化逻辑！这在 Gin 里是不可能的——你得手动 `gorm.Open()` 然后传来传去。

## Maven：构建工具

Maven 之于 Java/Kotlin 项目，相当于 `go mod` 之于 Go 项目 + `npm` 的一些能力。

| 操作 | Go | Maven |
|------|-----|-------|
| 定义依赖 | `go.mod` | `pom.xml` |
| 安装依赖 | `go mod download` | `./mvnw install` |
| 编译 | `go build` | `./mvnw compile` |
| 运行测试 | `go test ./...` | `./mvnw test` |
| 运行项目 | `go run main.go` | `./mvnw spring-boot:run` |
| 打包 | `go build -o app` | `./mvnw package` → 生成 `.jar` 文件 |

`./mvnw` 是 Maven Wrapper——它确保项目使用指定版本的 Maven，即使你机器上没装 Maven 也能用。类似 `npx`。

## 启动流程详解

当你执行 `./mvnw spring-boot:run` 时：

```
1. Maven 编译 Kotlin 代码
2. Spring Boot 启动
   ├── 读取 application.yaml 配置
   ├── 扫描所有 @Component/@Service/@Repository/@Controller 注解
   ├── 创建 Bean 实例并注入依赖
   ├── 初始化数据库连接池
   ├── 执行 Flyway 数据库迁移
   ├── 注册所有 @RequestMapping 路由
   ├── 执行 Bootstrap 组件（创建默认管理员）
   └── 启动 Tomcat Web 服务器（端口 8080）
3. 控制台输出 "Started EzposPlatformConsoleApplication"
```

对比 Gin 的启动：

```go
func main() {
    config := loadConfig()          // 手动加载配置
    db := connectDB(config)         // 手动连接数据库
    runMigrations(db)               // 手动执行迁移

    userRepo := NewUserRepo(db)     // 手动创建 repo
    userService := NewUserService(userRepo)  // 手动创建 service

    r := gin.Default()              // 手动创建路由
    r.POST("/users", userService.Create)  // 手动注册路由
    r.Run(":8080")                  // 手动启动服务器
}
```

Spring Boot 把上面所有"手动"的步骤全自动化了。这就是"约定优于配置"的威力。
