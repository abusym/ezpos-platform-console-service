# 01 — 项目结构详解

## 从 Go 项目结构说起

你写 Go 项目时，可能习惯这样组织：

```
my-go-project/
├── main.go
├── config/
├── handler/          ← 路由处理函数
├── service/          ← 业务逻辑
├── model/            ← 数据库模型
├── repository/       ← 数据库操作
├── middleware/        ← 中间件
└── go.mod
```

这叫**按技术层分包**——所有 handler 放一起，所有 model 放一起。

本项目用的是**按功能分包（Feature-first）**——每个业务模块把自己的 controller、service、entity 全部收在一起：

```
feature/
├── auth/         ← 认证相关的所有代码都在这里
├── user/         ← 用户相关的所有代码都在这里
├── merchant/     ← 商户相关的所有代码都在这里
└── release/      ← 版本发布相关的所有代码都在这里
```

**好处**：当你要改"商户"功能时，只需要看 `feature/merchant/` 这一个文件夹，不用在五六个文件夹之间跳来跳去。

## 每个 Feature 内部的分层

以 `feature/user/` 为例：

```
feature/user/
├── controller/
│   └── PlatformUsersController.kt    ← 接收 HTTP 请求
├── service/
│   └── PlatformUserService.kt        ← 业务逻辑
├── entity/
│   └── PlatformUser.kt               ← 数据库表映射
├── repository/
│   └── PlatformUserRepository.kt     ← 数据库查询
├── dto/
│   ├── PlatformUserDto.kt            ← 响应结构体
│   ├── CreatePlatformUserRequest.kt  ← 创建请求
│   ├── UpdatePlatformUserRequest.kt  ← 更新请求
│   └── ChangePasswordRequest.kt     ← 改密请求
├── mapper/
│   └── PlatformUserMapper.kt         ← Entity ↔ DTO 转换
├── bootstrap/
│   └── PlatformUserBootstrap.kt      ← 启动时创建默认管理员
└── security/
    └── UserPrincipalLoader.kt        ← 安全模块的 SPI 实现
```

### 请求处理流程

用一个类比帮你理解整个请求流程：

```
HTTP 请求进来
    ↓
Controller (前台接待)
    "你好，你要创建用户？让我检查一下你的请求格式对不对。"
    "格式没问题，我转给业务部门处理。"
    ↓
Service (业务部门)
    "让我看看这个用户名有没有被占用..."
    "没被占用，给密码加个密，然后让数据库部门存起来。"
    ↓
Repository (数据库部门)
    "好的，我把这条记录写入数据库。"
    ↓
Mapper (翻译官)
    "数据库返回的 Entity 里有些敏感字段（密码），
     我转换成 DTO 只保留该给客户端看的字段。"
    ↓
Controller 返回 DTO 给客户端
```

### 对应到 Gin 项目

| 本项目 | 你的 Gin 项目 | 职责 |
|--------|-------------|------|
| Controller | Handler 函数 | 解析请求 → 调 Service → 返回响应 |
| Service | Service 层（如果你有的话） | 业务逻辑、事务管理 |
| Repository | GORM 查询 | 数据库 CRUD |
| Entity | GORM Model | 数据库表结构映射 |
| DTO | 请求/响应的 struct | API 的输入输出结构 |
| Mapper | 手动赋值 | Entity 和 DTO 之间的字段转换 |

## common/ 公共模块

```
common/
├── config/
│   └── OpenApiConfig.kt           ← Swagger 文档配置
├── entity/
│   ├── base/
│   │   └── IdEntity.kt            ← 所有 Entity 的基类（提供 id + createdAt）
│   └── id/
│       ├── SnowflakeId.kt         ← 雪花 ID 注解
│       └── SnowflakeIdGenerator.kt ← 雪花 ID 生成器
├── exception/
│   └── BusinessExceptions.kt      ← 所有自定义异常
├── security/
│   ├── config/
│   │   └── SecurityConfig.kt      ← 安全配置（哪些路由要认证）
│   ├── filter/
│   │   └── OpaqueTokenAuthenticationFilter.kt  ← Token 验证过滤器
│   ├── model/
│   │   └── PlatformUserPrincipal.kt ← 认证主体对象
│   ├── current/
│   │   └── CurrentPrincipalProvider.kt ← 获取当前登录用户的接口
│   └── spi/
│       ├── TokenIntrospector.kt    ← Token 解析接口
│       └── PrincipalLoader.kt     ← 用户加载接口
└── web/
    └── problem/
        └── GlobalExceptionHandler.kt  ← 全局异常处理
```

### SPI 是什么？

SPI（Service Provider Interface）= **插件接口**。

类比：你定义了一个接口 `TokenIntrospector`（"谁能帮我查 Token？"），然后在 `feature/auth/` 模块里提供一个具体实现 `OpaqueTokenService`（"我能查！从 Redis 查！"）。

**为什么要这样？** `common/security/` 不应该依赖任何具体的 feature 模块。通过接口隔离，`common` 只定义"能力需求"，具体实现由 feature 模块提供。这样如果将来认证方式从 Redis Token 换成 JWT，你只需要换一个实现类，安全框架代码完全不用动。

## 关键源文件导读

### 1. 入口文件 — `EzposPlatformConsoleApplication.kt`

```kotlin
@SpringBootApplication
class EzposPlatformConsoleApplication

fun main(args: Array<String>) {
    runApplication<EzposPlatformConsoleApplication>(*args)
}
```

**相当于你的 `main.go`**，但极其简单——只有一个注解 `@SpringBootApplication`。这个注解告诉 Spring："从这个类所在的包开始，自动扫描所有子包里带注解的类，帮我全部初始化好。"

### 2. 基础实体 — `IdEntity.kt`

```kotlin
@MappedSuperclass
abstract class IdEntity {
    @Id
    @SnowflakeId
    var id: Long? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null
}
```

相当于你 GORM 里的 `gorm.Model`：

```go
// Go GORM
type Model struct {
    ID        uint `gorm:"primarykey"`
    CreatedAt time.Time
    UpdatedAt time.Time
}
```

所有 Entity 都继承这个基类，自动获得 `id`（雪花 ID）和 `createdAt`（创建时间）。

### 3. 配置文件 — `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ezpos   # 数据库连接
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate    # 只验证，不自动建表
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

相当于你的 `.env` 或 `config.yaml` 文件。Spring Boot 会自动读取并注入到需要的地方。

## 模块间的依赖关系

```
feature/auth
    ├── 依赖 → feature/user (通过 PrincipalLoader SPI)
    └── 依赖 → common/security

feature/release
    └── 依赖 → feature/release 内部的 ReleaseApplicationService

feature/subscription
    └── 依赖 → feature/merchant (验证商户存在)

feature/merchant
    └── 无外部依赖

feature/migration
    └── 无外部依赖

feature/audit
    └── 无外部依赖
```

**关键规则**：模块之间只通过 **Service 层** 互相调用，绝不直接访问别的模块的 Repository 或 Entity。这和你写前端组件时"只通过 props 和事件通信，不直接操作子组件内部状态"是一个道理。
