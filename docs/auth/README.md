# 认证与鉴权（Auth & Security）

本文档描述 `ezpos-platform-console-service` 当前的认证/鉴权实现，包括登录接口、token 形态、过滤器链路，以及在业务代码（service）中获取当前用户的推荐方式。

## 目录

- [概览](#概览)
- [认证主体（Principal）](#认证主体principal)
- [登录（Login）](#登录login)
- [Token：Opaque Token + Redis](#tokenopaque-token--redis)
- [请求鉴权流程（Filter Chain）](#请求鉴权流程filter-chain)
- [SPI 扩展点](#spi-扩展点)
- [在 service 中获取当前用户（推荐）](#在-service-中获取当前用户推荐)
- [常见问题](#常见问题)

## 概览

- **认证方式**：Bearer Token（Opaque token）
- **会话策略**：无状态（Stateless，不使用 HttpSession）
- **Token 存储**：Redis（token -> userId）
- **默认过期时间**：24h
- **过期策略**：滑动过期（token 每次被成功使用时会自动续期到 24h；即“24 小时无访问才过期”）

## 认证主体（Principal）

当前系统使用 `PlatformUserPrincipal` 作为 Spring Security principal：

- 类型：`net.ezpos.console.common.security.model.PlatformUserPrincipal`
- 关键字段：
  - `id`：平台用户 ID（业务侧唯一标识）
  - `username`：用户名（用于展示/审计）
  - `enabled`：是否启用
- 权限：当前 `authorities` 为空（仅区分“已认证/未认证”）。后续引入 RBAC/ABAC 可在此处装配权限。

## 登录（Login）

### 登录接口

- `POST /api/auth/login`

请求体：`LoginRequest`（username/password）  
响应：`LoginResponse`（accessToken/expiresInSeconds/user）

### 当前用户接口

- `GET /api/auth/me`

支持通过 `@AuthenticationPrincipal` 注入当前 principal。

## Token：Opaque Token + Redis

### 设计

- **Opaque token**：客户端拿到的是不可自解释的随机字符串，不包含用户信息
- 服务端保存映射：`token -> userId` 写入 Redis，并设置 TTL

### 配置项

位于 `application.yaml` 的 `ezpos.security.opaque-token`：

- `access-token-ttl`：默认 `24h`（可通过 `EZPOS_OPAQUE_TOKEN_TTL` 覆盖）
- `redis-key-prefix`：token key 前缀

### 滑动过期（自动续期）

当前实现为滑动过期：当 token 被成功解析出 userId 时，会对 Redis key 执行一次 `expire()`，把 TTL 续到 `access-token-ttl`。

## 请求鉴权流程（Filter Chain）

### 放行端点

以下端点不要求认证：

- `/api/auth/login`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### 鉴权流程

对于其余请求：

1. 从 `Authorization` 请求头提取 `Bearer <token>`
2. 通过 `TokenIntrospector` 解析 token 得到 `userId`
3. 通过 `PrincipalLoader` 根据 `userId` 加载 `PlatformUserPrincipal`
4. 构造 `Authentication` 写入 `SecurityContext`

无效/过期 token 会直接返回 401。

## SPI 扩展点

为了降低耦合，token 与用户加载通过 SPI 解耦：

- `TokenIntrospector`
  - 当前实现：`feature/auth/token/OpaqueTokenService`（Redis introspect + 滑动续期）
  - 可替换实现：JWT、远程 introspection、或对接统一认证中心
- `PrincipalLoader`
  - 当前实现：`feature/user/security/UserPrincipalLoader`（从平台用户表加载 + enabled 校验）

## 在 service 中获取当前用户（推荐）

### 注入 Provider（推荐）

如果你希望在 service 中直接使用“当前用户”，推荐注入以下 Provider：

#### 1) 获取 principal / userId（通用）

- 接口：`common/security/current/CurrentPrincipalProvider`
- 实现：`common/security/current/SpringSecurityCurrentPrincipalProvider`

常用方法：

- `requireUserId()`：未登录抛 401
- `principalOrNull()`：未登录返回 null

**使用示例（service 中直接获取 userId）**：

```kotlin
@Service
class MerchantService(
    private val current: CurrentPrincipalProvider,
) {
    fun myCurrentUserId(): Long = current.requireUserId()
}
```

**测试示例（无需启动 Spring / 无需真实登录）**：

```kotlin
// imports (示例)
// - net.ezpos.console.common.security.current.CurrentPrincipalProvider
// - net.ezpos.console.common.security.model.PlatformUserPrincipal
// - org.junit.jupiter.api.Test
// - org.junit.jupiter.api.Assertions.assertEquals
// - org.springframework.http.HttpStatus
// - org.springframework.web.server.ResponseStatusException
// - org.springframework.stereotype.Service

private class FakeCurrentPrincipalProvider(
    private val principal: PlatformUserPrincipal?,
) : CurrentPrincipalProvider {
    override fun principalOrNull(): PlatformUserPrincipal? = principal
    override fun requirePrincipal(): PlatformUserPrincipal =
        principal ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated")
}

@Test
fun `can test service without Spring Security`() {
    val service = MerchantService(
        FakeCurrentPrincipalProvider(
            PlatformUserPrincipal(
                id = 1001L,
                passwordHash = "ignored",
                enabled = true,
                usernameValue = "admin",
            ),
        ),
    )
    assertEquals(1001L, service.myCurrentUserId())
}
```

#### 获取 PlatformUser（需要查库）

推荐做法是：先从 `CurrentPrincipalProvider` 拿到 `userId`，再在业务模块内按需查询用户实体或 DTO。

示例（仅说明思路）：

```kotlin
val userId = current.requireUserId()
val user = platformUserRepository.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
```

## 常见问题

### 1) 为什么异步任务里取不到当前用户？

`SecurityContextHolder` 默认与线程绑定。如果在 `@Async` / 线程池 / 协程线程切换后使用，需要显式做 SecurityContext 传播，否则 Provider 会返回 null。

### 2) 我们后续要接入统一 JWT 登录怎么办？

保留 Filter 不变，替换 `TokenIntrospector` 的实现即可（同时按需要调整 `PrincipalLoader` 的加载来源/权限装配）。

