# 项目总览：从 Gin 到 Spring Boot 的思维转换

## 这个项目是什么？

EzPos Platform Console Service 是一个 **SaaS 管理后台**，用于管理商户、订阅、客户端版本发布等。你可以把它想象成一个"运营管理中台"。

**核心功能模块：**

| 模块 | 功能 | 类比 |
|------|------|------|
| auth | 登录/登出/获取当前用户 | 你写过的登录接口 |
| user | 平台管理员 CRUD | 后台用户管理 |
| merchant | 商户管理 | 商家信息管理 |
| subscription | 订阅计划管理 | 类似 SaaS 套餐 |
| release | 客户端版本发布 | App 更新管理 |
| migration | 数据迁移任务 | 数据导入导出 |
| audit | 操作审计日志 | 操作日志记录 |

## 技术栈对照

| 维度 | 你熟悉的 (Go) | 本项目 (Kotlin/Spring) |
|------|--------------|----------------------|
| 语言 | Go | **Kotlin**（运行在 JVM 上，类似 TypeScript 之于 JavaScript） |
| Web 框架 | Gin | **Spring Boot**（更重量级，但自动化程度更高） |
| ORM | GORM | **JPA / Hibernate**（概念类似，写法不同） |
| 路由 | `r.GET("/users", handler)` | **`@GetMapping("/users")`** 注解 |
| 中间件 | `r.Use(middleware)` | **Filter / Interceptor** |
| 配置 | `.env` / `config.yaml` | **`application.yaml`** |
| 数据库迁移 | golang-migrate / GORM AutoMigrate | **Flyway**（SQL 文件迁移） |
| 依赖注入 | 手动 wire / fx | **Spring IoC 容器**（全自动） |
| 构建工具 | `go build` | **Maven**（`./mvnw`） |

## 核心思维差异

### 1. "约定优于配置" vs "显式编码"

Go/Gin 的风格是**显式**的——你手动注册路由、手动初始化依赖、手动组装一切：

```go
// Go: 你得自己组装一切
db := connectDB()
userRepo := NewUserRepo(db)
userService := NewUserService(userRepo)
r.GET("/users", userService.List)
```

Spring Boot 的风格是**约定**的——你只需要打标记（注解），框架帮你组装：

```kotlin
// Kotlin/Spring: 打上注解，框架自动搞定
@RestController
@RequestMapping("/api/users")
class UsersController(private val userService: UserService) {
    @GetMapping
    fun list(pageable: Pageable) = userService.list(pageable)
}
```

你不需要手动 `new UsersController(userService)` —— Spring 看到 `@RestController` 注解后，会自动创建这个对象，并且自动把 `UserService` 注入进来。

### 2. 注解驱动 vs 函数调用

在 Gin 里，你通过**函数调用**来定义行为：

```go
r.POST("/login", authHandler.Login)  // 显式注册路由
```

在 Spring 里，你通过**注解（Annotation）**来声明行为：

```kotlin
@PostMapping("/login")  // 注解声明路由
fun login(@RequestBody request: LoginRequest): LoginResponse { ... }
```

**注解 = 给代码贴标签**。框架在启动时扫描这些标签，自动执行相应的逻辑。你可以把注解理解成 TypeScript 的装饰器（Decorator），功能一样。

### 3. 项目启动流程

```
Gin 项目:
main() → 初始化配置 → 连接数据库 → 创建各层实例 → 注册路由 → 启动 HTTP 服务器

Spring Boot 项目:
main() → Spring 容器启动 → 自动扫描所有注解 → 自动创建 Bean → 自动注册路由 → 启动 HTTP 服务器
```

Spring 把你在 Go 里手动做的事情全自动化了。**你只需要关注业务逻辑，框架帮你处理"组装"和"连线"。**

## 项目目录结构一览

```
src/main/kotlin/net/ezpos/console/
├── EzposPlatformConsoleApplication.kt    ← 入口（相当于 main.go）
├── common/                               ← 公共模块（工具、配置、安全）
│   ├── config/                           ← 全局配置
│   ├── entity/                           ← 基础实体类
│   ├── exception/                        ← 自定义异常
│   ├── security/                         ← 安全认证
│   └── web/                              ← 全局异常处理
└── feature/                              ← 业务功能模块
    ├── auth/                             ← 认证模块
    ├── user/                             ← 用户管理
    ├── merchant/                         ← 商户管理
    ├── subscription/                     ← 订阅管理
    ├── release/                          ← 版本发布
    ├── migration/                        ← 数据迁移
    └── audit/                            ← 审计日志
```

每个 feature 模块内部结构一致：

```
feature/user/
├── controller/    ← 路由处理（≈ Gin Handler）
├── service/       ← 业务逻辑
├── repository/    ← 数据库操作（≈ GORM 查询）
├── entity/        ← 数据库模型（≈ GORM Model）
├── dto/           ← 请求/响应结构体
└── mapper/        ← Entity ↔ DTO 转换
```

> **为什么要分这么多层？** 在 Gin 项目中你可能习惯 handler 直接调 GORM。Spring 生态强调**分层解耦**：Controller 只处理 HTTP，Service 处理业务逻辑，Repository 只做数据库操作。这样每层都可以独立测试和替换。

## 接下来的学习路径

建议按以下顺序阅读：

1. **[01-project-structure.md](01-project-structure.md)** — 深入理解项目结构和分层
2. **[02-spring-boot-basics.md](02-spring-boot-basics.md)** — Spring Boot 核心概念
3. **[03-dependency-injection.md](03-dependency-injection.md)** — 依赖注入详解
4. **[04-database-and-orm.md](04-database-and-orm.md)** — JPA vs GORM
5. **[05-api-and-controllers.md](05-api-and-controllers.md)** — Controller 和路由
6. **[06-authentication.md](06-authentication.md)** — 认证系统详解
7. **[07-error-handling.md](07-error-handling.md)** — 异常处理机制
8. **[08-testing.md](08-testing.md)** — 测试方法
9. **[09-build-and-run.md](09-build-and-run.md)** — 构建和运行
