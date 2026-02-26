# Web 错误响应（ProblemDetail）

本文档描述 `ezpos-platform-console-service` 的 **统一异常返回** 方案：使用 Spring Framework 提供的 `ProblemDetail`（RFC 7807）作为 API 的错误响应格式。

> 目标：让业务异常、参数校验异常、以及 Spring Security 的 401/403 都返回一致的 `application/problem+json`。

## 1. 返回格式（RFC 7807）

当请求失败（4xx/5xx）时，服务端会返回 `Content-Type: application/problem+json`，响应体为 `ProblemDetail`。

常见字段（标准字段）：

- `type`：问题类型（当前为 `about:blank`）
- `title`：简短标题（通常为 HTTP reason phrase，如 `Unauthorized`/`Bad Request`）
- `status`：HTTP 状态码（如 400/401/403/409/500）
- `detail`：更具体的错误描述
- `instance`：发生错误的请求路径（request URI）

### 校验错误（额外字段）

当请求体/参数校验失败时（例如 `@Valid`），会额外返回：

- `errors`：数组，元素形如 `{ "field": "...", "message": "..." }` 或 `{ "path": "...", "message": "..." }`

## 2. 代码位置

与 Web/HTTP 强相关的异常映射与输出组件放在：

- `src/main/kotlin/net/ezpos/console/common/web/problem/*`

其中包含：

- `GlobalExceptionHandler`：`@RestControllerAdvice`，将常见异常映射为 `ProblemDetail`
- `ProblemDetailAuthenticationEntryPoint`：统一 401（未认证）输出
- `ProblemDetailAccessDeniedHandler`：统一 403（无权限）输出
- `ProblemDetailWriter`：将 `ProblemDetail` 写入响应（JSON + `application/problem+json`）

## 3. 如何在业务代码中抛出错误

推荐在 service/controller 中直接抛 `ResponseStatusException`：

```kotlin
throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User disabled")
```

该异常会被全局异常处理捕获并转换成 `ProblemDetail`（HTTP 401 + `application/problem+json`）。

## 4. 安全相关（401/403）

### 4.1 未登录访问受保护接口

当访问需要认证的接口但未携带有效认证信息时，Spring Security 会触发 401，输出 `ProblemDetail`。

### 4.2 无权限

当已认证但权限不足时，Spring Security 会触发 403，输出 `ProblemDetail`。

### 4.3 无效/过期 token

`OpaqueTokenAuthenticationFilter` 在解析到无效/过期 token 时会触发统一的 401 输出，保证不会出现“只有 status 没有 body”的情况。

## 5. 配置项

在 `application.yaml` 中启用了 ProblemDetail 支持：

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

## 6. 约定与边界

- **只统一错误响应**：成功响应（2xx）仍返回各自业务 DTO，不强制包一层通用 `ApiResponse`
- **不引入错误码**：当前阶段不要求 `code`；若未来需要区分更细的业务分支，可在 `ProblemDetail.properties` 中扩展字段

