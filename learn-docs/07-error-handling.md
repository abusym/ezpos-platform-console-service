# 07 — 异常处理机制

## Go 的 error vs Kotlin 的 Exception

这是两种语言最大的哲学差异之一。

### Go：显式错误返回

```go
func (s *UserService) GetById(id int64) (*UserDTO, error) {
    user, err := s.repo.FindById(id)
    if err != nil {
        return nil, fmt.Errorf("用户不存在: %w", err)
    }
    return toDTO(user), nil
}

// 调用方必须处理 error
user, err := userService.GetById(123)
if err != nil {
    c.JSON(404, gin.H{"error": err.Error()})
    return
}
```

### Kotlin/Spring：异常抛出

```kotlin
fun getById(id: Long): MerchantDto {
    val merchant = repository.findById(id)
        .orElseThrow { EntityNotFoundException("Merchant", id) }
    //                 ^^^^^^^^^^^^^^^^^^^^^^^^ 直接抛异常，不需要返回 error
    return mapper.toDto(merchant)
}

// 调用方不需要处理异常——框架统一捕获
```

**关键区别**：
- Go 要求你**每一层**都处理 error 并往上传递
- Kotlin 的异常会自动**向上冒泡**，直到被某个地方捕获
- Spring 的 `GlobalExceptionHandler` 在最外层统一捕获所有异常

## 本项目的异常体系

```
BusinessException (基类)
├── EntityNotFoundException        → 404 Not Found
├── EntityAlreadyExistsException   → 409 Conflict
├── BusinessRuleException          → 400 Bad Request
├── AuthenticationFailedException  → 401 Unauthorized
└── DataIntegrityException         → 500 Internal Server Error
```

### 异常定义（BusinessExceptions.kt）

```kotlin
// 所有业务异常的基类
open class BusinessException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// 实体不存在
class EntityNotFoundException(entity: String, identifier: Any) :
    BusinessException("$entity not found: $identifier")
// 使用：throw EntityNotFoundException("Merchant", 123)
// 效果：HTTP 404 + { "detail": "Merchant not found: 123" }

// 实体已存在（唯一约束冲突）
class EntityAlreadyExistsException(message: String) :
    BusinessException(message)
// 使用：throw EntityAlreadyExistsException("Username 'admin' already exists")
// 效果：HTTP 409

// 业务规则违反
class BusinessRuleException(message: String) :
    BusinessException(message)
// 使用：throw BusinessRuleException("Version must follow semver format")
// 效果：HTTP 400

// 认证失败
class AuthenticationFailedException(message: String) :
    BusinessException(message)
// 使用：throw AuthenticationFailedException("Invalid credentials")
// 效果：HTTP 401
```

### 全局异常处理（GlobalExceptionHandler）

```kotlin
@RestControllerAdvice  // 全局异常捕获器（类似 Gin 的 Recovery 中间件，但更强大）
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.message ?: "Not found"
        )
    }

    @ExceptionHandler(EntityAlreadyExistsException::class)
    fun handleConflict(ex: EntityAlreadyExistsException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.message ?: "Already exists"
        )
    }

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBadRequest(ex: BusinessRuleException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message ?: "Bad request"
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        // 处理 @Valid 校验失败
        val detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        detail.detail = "Validation failed"
        detail.setProperty("errors", ex.bindingResult.fieldErrors.map {
            mapOf("field" to it.field, "message" to it.defaultMessage)
        })
        return detail
    }
}
```

### 响应格式：RFC 9457 ProblemDetail

所有错误响应统一使用 **ProblemDetail** 格式：

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Merchant not found: 123",
  "instance": "/api/merchants/123"
}
```

校验错误带字段明细：

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    { "field": "name", "message": "must not be blank" },
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

## 对比 Gin 的错误处理方式

### Gin：每个 Handler 都得处理错误

```go
func (h *MerchantHandler) GetById(c *gin.Context) {
    id, err := strconv.ParseInt(c.Param("id"), 10, 64)
    if err != nil {
        c.JSON(400, gin.H{"error": "invalid id"})
        return
    }

    merchant, err := h.service.GetById(id)
    if err != nil {
        if errors.Is(err, ErrNotFound) {
            c.JSON(404, gin.H{"error": err.Error()})  // 手动判断错误类型
        } else {
            c.JSON(500, gin.H{"error": "internal error"})
        }
        return
    }

    c.JSON(200, merchant)
}
```

### Spring：Controller 完全不处理错误

```kotlin
@GetMapping("/{id}")
fun getById(@PathVariable id: Long): MerchantDto {
    return service.getById(id)
    // 如果 service 抛出 EntityNotFoundException
    // → GlobalExceptionHandler 自动捕获
    // → 自动返回 404 + ProblemDetail JSON
    // Controller 完全不需要 try-catch！
}
```

**这就是为什么 Spring Controller 代码如此简洁**——所有错误处理都集中在 `GlobalExceptionHandler` 里，每个 Controller 方法只需要关注正常流程。

## 异常使用的实际例子

### Service 层抛异常

```kotlin
@Service
class MerchantService(
    private val repository: MerchantRepository,
    private val mapper: MerchantMapper
) {
    fun create(request: CreateMerchantRequest): MerchantDto {
        // 检查名称唯一性
        if (repository.existsByName(request.name)) {
            throw EntityAlreadyExistsException(
                "Merchant with name '${request.name}' already exists"
            )
            // → 自动返回 409
        }

        val merchant = Merchant(name = request.name, ...)
        return mapper.toDto(repository.save(merchant))
    }

    fun getById(id: Long): MerchantDto {
        val merchant = repository.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }
            // → 自动返回 404
        return mapper.toDto(merchant)
    }

    fun enable(id: Long): MerchantDto {
        val merchant = repository.findById(id)
            .orElseThrow { EntityNotFoundException("Merchant", id) }

        if (merchant.enabled) {
            throw BusinessRuleException("Merchant is already enabled")
            // → 自动返回 400
        }

        merchant.enabled = true
        return mapper.toDto(repository.save(merchant))
    }
}
```

### 整个请求链路的异常流向

```
客户端请求 GET /api/merchants/999
    ↓
Controller.getById(999)
    ↓
Service.getById(999)
    ↓
Repository.findById(999) → 找不到
    ↓
throw EntityNotFoundException("Merchant", 999)  ← 异常从这里抛出
    ↑                                            ↓
Service 不处理，异常冒泡                           ↓
    ↑                                            ↓
Controller 不处理，异常继续冒泡                     ↓
    ↑                                            ↓
GlobalExceptionHandler 捕获！                      ↓
    ↓
返回 HTTP 404:
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Merchant not found: 999"
}
```

## 对比总结

| 维度 | Go/Gin | Spring |
|------|--------|--------|
| 错误传播 | 手动 return error，逐层传递 | 异常自动冒泡 |
| 错误处理位置 | 分散在每个 Handler 里 | 集中在 GlobalExceptionHandler |
| 错误类型判断 | `errors.Is()` / `errors.As()` | `@ExceptionHandler(XxxException::class)` |
| 响应格式 | 自定义 JSON | 标准 ProblemDetail（RFC 9457） |
| 代码量 | 每个接口都要 if err != nil | Controller 零错误处理代码 |

Go 的显式错误处理更"诚实"——你能看到每个可能出错的地方。Spring 的异常机制更"优雅"——业务代码更干净，但需要理解异常冒泡机制。

两种方式没有优劣之分，只是风格不同。关键是**在项目中保持一致**。
