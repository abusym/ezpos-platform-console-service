# 05 — Controller 和路由：从 Gin Handler 到 Spring Controller

## 整体对比

### Gin 路由注册

```go
func main() {
    r := gin.Default()

    auth := r.Group("/api/auth")
    {
        auth.POST("/login", authHandler.Login)
        auth.GET("/me", authMiddleware(), authHandler.Me)
        auth.POST("/logout", authMiddleware(), authHandler.Logout)
    }

    users := r.Group("/api/platform-users", authMiddleware())
    {
        users.POST("/", userHandler.Create)
        users.GET("/", userHandler.List)
        users.GET("/:id", userHandler.GetById)
        users.PATCH("/:id", userHandler.Update)
    }

    r.Run(":8080")
}
```

### Spring Controller 注解式路由

```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        return authService.login(request)
    }

    @GetMapping("/me")
    fun me(): PlatformUserInfo {
        return authService.me()
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader("Authorization") authorization: String) {
        val token = authorization.removePrefix("Bearer ")
        authService.logout(token)
    }
}
```

**关键差异**：
- Gin 是**集中式路由注册**（在一个地方注册所有路由）
- Spring 是**分散式注解声明**（每个 Controller 自带路由定义）

两种方式各有优劣，但实际效果完全一样。

## Controller 详解

### 完整的 CRUD Controller

以 `MerchantsController` 为例，这是最标准的 CRUD 控制器：

```kotlin
@RestController                                    // ① 声明为 REST 控制器
@RequestMapping("/api/merchants")                  // ② 基础路径
@Tag(name = "商户管理")                              // ③ Swagger 文档标签
class MerchantsController(
    private val service: MerchantService            // ④ 注入 Service
) {

    @PostMapping                                   // POST /api/merchants
    @ResponseStatus(HttpStatus.CREATED)            // 返回 201 状态码
    @Operation(summary = "创建商户")                 // Swagger 文档
    fun create(
        @Valid @RequestBody request: CreateMerchantRequest  // 从 body 解析 + 校验
    ): MerchantDto {
        return service.create(request)
    }

    @GetMapping                                    // GET /api/merchants
    @Operation(summary = "分页查询商户列表")
    fun list(pageable: Pageable): Page<MerchantDto> {  // 自动解析分页参数
        return service.list(pageable)
    }

    @GetMapping("/{id}")                           // GET /api/merchants/:id
    @Operation(summary = "根据 ID 查询商户详情")
    fun getById(@PathVariable id: Long): MerchantDto {  // 从 URL 路径取 id
        return service.getById(id)
    }

    @PatchMapping("/{id}")                         // PATCH /api/merchants/:id
    @Operation(summary = "更新商户信息")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateMerchantRequest
    ): MerchantDto {
        return service.update(id, request)
    }

    @PostMapping("/{id}:enable")                   // POST /api/merchants/:id:enable
    @Operation(summary = "启用商户")
    fun enable(@PathVariable id: Long): MerchantDto {
        return service.enable(id)
    }

    @PostMapping("/{id}:disable")                  // POST /api/merchants/:id:disable
    @Operation(summary = "禁用商户")
    fun disable(@PathVariable id: Long): MerchantDto {
        return service.disable(id)
    }
}
```

### 逐行翻译成 Gin

上面的 Controller 用 Gin 写：

```go
type MerchantsHandler struct {
    service *MerchantService
}

func (h *MerchantsHandler) Register(r *gin.RouterGroup) {
    g := r.Group("/merchants")
    g.POST("/", h.Create)
    g.GET("/", h.List)
    g.GET("/:id", h.GetById)
    g.PATCH("/:id", h.Update)
    g.POST("/:id/enable", h.Enable)
    g.POST("/:id/disable", h.Disable)
}

func (h *MerchantsHandler) Create(c *gin.Context) {
    var req CreateMerchantRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(400, gin.H{"error": err.Error()})
        return
    }
    result, err := h.service.Create(&req)
    if err != nil {
        handleError(c, err)
        return
    }
    c.JSON(201, result)
}

func (h *MerchantsHandler) List(c *gin.Context) {
    page, _ := strconv.Atoi(c.DefaultQuery("page", "0"))
    size, _ := strconv.Atoi(c.DefaultQuery("size", "20"))
    result, err := h.service.List(page, size)
    if err != nil {
        handleError(c, err)
        return
    }
    c.JSON(200, result)
}

func (h *MerchantsHandler) GetById(c *gin.Context) {
    id, _ := strconv.ParseInt(c.Param("id"), 10, 64)
    result, err := h.service.GetById(id)
    if err != nil {
        handleError(c, err)
        return
    }
    c.JSON(200, result)
}
```

**对比后你会发现**：Spring Controller 代码量远少于 Gin Handler，因为框架自动处理了：
- JSON 绑定（不需要 `ShouldBindJSON`）
- 参数解析（不需要 `c.Param`、`c.Query`）
- 参数校验（不需要手动 validate）
- 响应序列化（不需要 `c.JSON`）
- 错误处理（不需要每个方法都 `handleError`）

## 请求参数绑定

### @RequestBody — 绑定 JSON 请求体

```kotlin
// DTO 定义
data class CreateMerchantRequest(
    @field:NotBlank(message = "商户名称不能为空")     // 校验注解
    @Schema(description = "商户名称", example = "张三的小店")
    val name: String,

    @Schema(description = "联系人姓名")
    val contactName: String? = null,                // 可选字段

    @Schema(description = "联系电话")
    val contactPhone: String? = null
)

// Controller 中使用
@PostMapping
fun create(@Valid @RequestBody request: CreateMerchantRequest): MerchantDto
//          ^^^^^ 触发校验   ^^^^^^^^^^^^ 从 body 解析
```

Go 对比：

```go
type CreateMerchantRequest struct {
    Name         string `json:"name" binding:"required"`
    ContactName  string `json:"contactName"`
    ContactPhone string `json:"contactPhone"`
}
```

### @PathVariable — 绑定路径参数

```kotlin
@GetMapping("/{id}")
fun getById(@PathVariable id: Long): MerchantDto
// 请求 GET /api/merchants/123 → id = 123
```

Go 对比：`c.Param("id")`

### @RequestParam — 绑定查询参数

```kotlin
@GetMapping("/expiring")
fun getExpiring(
    @RequestParam(defaultValue = "30") days: Int
): List<SubscriptionDto>
// 请求 GET /api/subscriptions/expiring?days=7 → days = 7
// 请求 GET /api/subscriptions/expiring         → days = 30（默认值）
```

Go 对比：`c.DefaultQuery("days", "30")`

### @RequestHeader — 绑定请求头

```kotlin
@PostMapping("/logout")
fun logout(@RequestHeader("Authorization") authorization: String)
```

Go 对比：`c.GetHeader("Authorization")`

### Pageable — 自动分页参数

```kotlin
@GetMapping
fun list(pageable: Pageable): Page<MerchantDto>
// Spring 自动从 ?page=0&size=20&sort=createdAt,desc 解析
```

你不需要做任何事情，Spring 自动把 query string 解析成 `Pageable` 对象。

## 请求校验 (@Valid)

```kotlin
data class CreatePlatformUserRequest(
    @field:NotBlank       // 不能为空字符串
    val username: String,

    @field:Size(min = 6)  // 最少 6 个字符
    val password: String,

    val displayName: String? = null,

    @field:Email           // 必须是邮箱格式
    val email: String? = null
)
```

如果校验失败，Spring 自动返回 400 错误：

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    { "field": "username", "message": "must not be blank" },
    { "field": "password", "message": "size must be between 6 and 2147483647" }
  ]
}
```

对比 Gin 的 `binding:"required,min=6"` tag。概念一样，只是注解语法不同。

## 响应状态码

```kotlin
@PostMapping
@ResponseStatus(HttpStatus.CREATED)    // 返回 201
fun create(...): MerchantDto

@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT) // 返回 204（无响应体）
fun logout(...)

// 不加 @ResponseStatus 默认返回 200
@GetMapping
fun list(...): Page<MerchantDto>       // 返回 200
```

Gin 对比：`c.JSON(201, result)` 中的 201。

## 特殊的 Action 端点

本项目有些"动作"端点使用了 Google API 风格的冒号语法：

```kotlin
@PostMapping("/{id}:publish")    // POST /api/releases/123:publish
fun publish(@PathVariable id: Long): ReleaseDto

@PostMapping("/{id}:pause")     // POST /api/releases/123:pause
fun pause(@PathVariable id: Long): ReleaseDto

@PostMapping("/{id}:renew")     // POST /api/subscriptions/456:renew
fun renew(@PathVariable id: Long, @RequestBody request: RenewRequest): SubscriptionDto
```

这些不是 CRUD 操作，而是对资源的"动作"。用冒号分隔是 Google API 的设计惯例。

## 客户端更新检查端点（公开 API）

```kotlin
@RestController
@RequestMapping("/api/client-updates")
class ClientUpdatesController(private val service: ClientUpdateService) {

    @GetMapping("/check")
    fun check(
        @RequestHeader("X-App-Code") appCode: String,
        @RequestHeader("X-Platform") platform: String,
        @RequestHeader("X-Current-Version") currentVersion: String,
        @RequestHeader("X-Tenant-Id") tenantId: String,
        @RequestHeader("X-Device-Id", required = false) deviceId: String?
    ): ClientUpdateCheckResponse {
        return service.check(appCode, platform, currentVersion, tenantId, deviceId)
    }
}
```

这个端点不需要认证（在 `SecurityConfig` 中配置为公开），客户端通过请求头传入参数。

## 本项目所有 API 一览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| **Auth** | POST | `/api/auth/login` | 登录 |
| | GET | `/api/auth/me` | 获取当前用户 |
| | POST | `/api/auth/logout` | 登出 |
| **Users** | POST | `/api/platform-users` | 创建用户 |
| | GET | `/api/platform-users` | 用户列表 |
| | GET | `/api/platform-users/{id}` | 用户详情 |
| | PATCH | `/api/platform-users/{id}` | 更新用户 |
| | PATCH | `/api/platform-users/{id}/password` | 改密 |
| **Merchants** | POST | `/api/merchants` | 创建商户 |
| | GET | `/api/merchants` | 商户列表 |
| | GET | `/api/merchants/{id}` | 商户详情 |
| | PATCH | `/api/merchants/{id}` | 更新商户 |
| | POST | `/api/merchants/{id}:enable` | 启用 |
| | POST | `/api/merchants/{id}:disable` | 禁用 |
| **Plans** | POST | `/api/plans` | 创建套餐 |
| | GET | `/api/plans` | 套餐列表 |
| | PATCH | `/api/plans/{id}` | 更新套餐 |
| **Subscriptions** | POST | `/api/subscriptions` | 创建订阅 |
| | GET | `/api/subscriptions` | 订阅列表 |
| | POST | `/api/subscriptions/{id}:renew` | 续费 |
| | GET | `/api/subscriptions/expiring` | 即将到期 |
| **Release Apps** | POST | `/api/release-applications` | 创建应用 |
| | GET | `/api/release-applications` | 应用列表 |
| | GET | `/api/release-applications/{code}` | 应用详情 |
| | PATCH | `/api/release-applications/{code}` | 更新应用 |
| **Releases** | POST | `/api/releases` | 创建版本 |
| | GET | `/api/releases` | 版本列表 |
| | GET | `/api/releases/{id}` | 版本详情 |
| | PATCH | `/api/releases/{id}` | 更新版本 |
| | POST | `/api/releases/{id}:publish` | 发布 |
| | POST | `/api/releases/{id}:pause` | 暂停 |
| | POST | `/api/releases/{id}:resume` | 恢复 |
| | POST | `/api/releases/{id}/artifact:complete` | 完成上传 |
| **Client Updates** | GET | `/api/client-updates/check` | 检查更新（公开） |
| | POST | `/api/client-updates/report` | 上报状态（公开） |
| **Migrations** | POST | `/api/data-migrations` | 创建迁移 |
| | GET | `/api/data-migrations` | 迁移列表 |
| | GET | `/api/data-migrations/{id}` | 迁移详情 |
| **Audit** | GET | `/api/audit-logs` | 审计日志 |
