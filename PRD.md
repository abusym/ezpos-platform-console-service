# EzPos 平台总后台 — 产品需求文档 (PRD)

> **版本**: 1.0.0
> **最后更新**: 2026-03-01
> **状态**: 当前已实现功能梳理

---

## 目录

- [1. 产品概述](#1-产品概述)
- [2. 用户角色](#2-用户角色)
- [3. 功能模块总览](#3-功能模块总览)
- [4. 模块一：认证与授权 (Auth)](#4-模块一认证与授权-auth)
- [5. 模块二：平台用户管理 (User)](#5-模块二平台用户管理-user)
- [6. 模块三：商家管理 (Merchant)](#6-模块三商家管理-merchant)
- [7. 模块四：套餐与订阅管理 (Subscription & Plan)](#7-模块四套餐与订阅管理-subscription--plan)
- [8. 模块五：版本发布与客户端更新 (Release)](#8-模块五版本发布与客户端更新-release)
- [9. 模块六：数据迁移 (Migration)](#9-模块六数据迁移-migration)
- [10. 模块七：审计日志 (Audit)](#10-模块七审计日志-audit)
- [11. 全局规范：错误响应](#11-全局规范错误响应)
- [12. 全局规范：认证机制](#12-全局规范认证机制)
- [13. 非功能性需求](#13-非功能性需求)
- [14. 数据模型总览](#14-数据模型总览)
- [15. API 清单汇总](#15-api-清单汇总)
- [16. TODO：尚未实现的功能清单](#16-todo尚未实现的功能清单)

---

## 1. 产品概述

### 1.1 产品定位

**EzPos 平台总后台** 是一个面向 EzPos SaaS POS 系统的**平台级管理后台服务**。它不面向终端商家用户，而是面向 **EzPos 平台运营团队**，用于管理商家入驻、订阅套餐、客户端版本发布、数据迁移和运营审计等平台级运营工作。

### 1.2 核心价值

| 能力 | 说明 |
|------|------|
| 商家全生命周期管理 | 商家创建、信息维护、启用/停用 |
| SaaS 订阅管理 | 套餐定义、订阅开通、续费、到期预警 |
| 客户端版本管理 | 多应用多平台版本发布、灰度发布、强制更新、紧急暂停 |
| 数据迁移 | 商家间数据迁移任务的创建与跟踪 |
| 运营审计 | 全局操作审计日志查询 |
| 平台用户管理 | 运营人员账号管理、密码管理 |

### 1.3 技术栈

- **语言**: Kotlin 2.2 / Java 24
- **框架**: Spring Boot 4.0.3
- **数据库**: PostgreSQL（Flyway 管理迁移）
- **缓存/Token 存储**: Redis
- **ID 生成**: 雪花算法 (Snowflake ID)
- **构建工具**: Maven
- **API 文档**: Swagger / OpenAPI 3.0

---

## 2. 用户角色

| 角色 | 说明 | 认证方式 |
|------|------|----------|
| **平台管理员** | EzPos 运营团队成员，通过后台登录管理所有功能模块 | 用户名 + 密码登录，获取 Bearer Token |
| **客户端应用** | EzPos POS 客户端程序，用于检查版本更新和上报更新状态 | 无需登录，通过请求头传递应用标识 |

> **说明**: 当前系统未实现 RBAC（角色权限控制），所有已认证的平台用户拥有相同权限。`PlatformUserPrincipal.authorities` 字段预留为空，后续可扩展。

---

## 3. 功能模块总览

```
┌─────────────────────────────────────────────────────────┐
│                    EzPos 平台总后台                       │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│   认证   │ 用户管理  │ 商家管理  │ 订阅管理  │  版本发布    │
│  (Auth)  │  (User)  │(Merchant)│  (Sub)   │  (Release)  │
├──────────┴──────────┼──────────┼──────────┼─────────────┤
│     数据迁移         │ 审计日志  │ 客户端    │             │
│    (Migration)      │ (Audit)  │ 更新检查  │             │
└─────────────────────┴──────────┴──────────┴─────────────┘
```

---

## 4. 模块一：认证与授权 (Auth)

### 4.1 功能描述

平台管理员通过用户名和密码登录后台系统，系统签发不透明令牌 (Opaque Token)，后续所有 API 请求通过 Bearer Token 进行身份验证。

### 4.2 功能清单

| 功能 | 说明 |
|------|------|
| 登录 | 用户名 + 密码验证，签发 Token |
| 获取当前用户信息 | 根据 Token 返回当前登录用户详情 |
| 登出 | 撤销当前 Token，立即失效 |

### 4.3 业务规则

1. **密码验证**: 使用 BCrypt 加密算法对比密码。
2. **账号启用检查**: 登录时校验账号是否为启用状态 (`enabled = true`)，禁用账号无法登录。
3. **Token 签发**:
   - 使用 32 字节密码学安全随机数生成 Base64 URL 安全编码 Token。
   - Token 存储在 Redis 中，Key 格式为 `ezpos:platform:opaque-token:{token}`，Value 为 `userId`。
   - 默认 TTL 为 **24 小时**。
4. **滑动过期**: 每次 Token 验证成功后，TTL 自动续期到 24 小时（"24 小时无操作过期"策略）。
5. **Token 撤销**: 登出时直接从 Redis 中删除对应 Token 记录。

### 4.4 API 详情

#### 4.4.1 登录

```
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "admin",       // 必填，非空
  "password": "123456"       // 必填，非空
}
```

**成功响应** (200):
```json
{
  "tokenType": "Bearer",
  "accessToken": "dGhpcyBpcyBhIHRva2Vu...",
  "expiresInSeconds": 86400,
  "user": {
    "id": 1234567890,
    "username": "admin",
    "displayName": "Administrator",
    "email": "admin@ezpos.net",
    "enabled": true
  }
}
```

**失败场景**:
| 场景 | HTTP 状态码 | detail |
|------|------------|--------|
| 用户名不存在 | 401 | Authentication failed |
| 密码错误 | 401 | Authentication failed |
| 账号已禁用 | 401 | Authentication failed |

#### 4.4.2 获取当前用户信息

```
GET /api/auth/me
Authorization: Bearer {token}
```

**成功响应** (200):
```json
{
  "id": 1234567890,
  "username": "admin",
  "displayName": "Administrator",
  "email": "admin@ezpos.net",
  "enabled": true
}
```

#### 4.4.3 登出

```
POST /api/auth/logout
Authorization: Bearer {token}
```

**成功响应**: 200 (无响应体)

### 4.5 初始化数据

系统启动时自动检查并创建默认管理员账号：

| 字段 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `123456` |
| 显示名 | `Administrator` |

> 仅在 `admin` 用户不存在时执行创建，已存在则跳过。

---

## 5. 模块二：平台用户管理 (User)

### 5.1 功能描述

管理平台运营团队的用户账号，包括创建、查询、更新用户信息和修改密码。

### 5.2 功能清单

| 功能 | 说明 |
|------|------|
| 创建用户 | 新建平台管理员账号 |
| 用户列表 | 分页查询所有用户 |
| 用户详情 | 根据 ID 查询单个用户 |
| 更新用户 | 修改显示名、邮箱、启用状态 |
| 修改密码 | 验证旧密码后设置新密码 |

### 5.3 业务规则

1. **用户名唯一性**: 创建用户时检查用户名是否已存在，重复则返回 409 Conflict。
2. **密码加密**: 创建用户和修改密码时均使用 BCrypt 加密存储。
3. **默认启用**: 新创建的用户默认 `enabled = true`。
4. **修改密码前置校验**: 必须正确提供旧密码才能设置新密码。
5. **空值处理**: 更新用户时，如果 `displayName` 或 `email` 传入空字符串，系统自动置为 `null`（表示清除该字段）。
6. **部分更新语义**: 更新接口采用 PATCH 语义，仅传入的字段会被修改，未传入的字段保持不变。

### 5.4 数据模型

**platform_users 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | 用户 ID |
| username | VARCHAR(64) | UNIQUE, NOT NULL | 登录用户名 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt 密码哈希 |
| display_name | VARCHAR(128) | 可空 | 显示名称 |
| email | VARCHAR(128) | 可空 | 邮箱地址 |
| enabled | BOOLEAN | DEFAULT true | 启用状态 |
| updated_at | TIMESTAMPTZ | 自动更新 | 最后修改时间 |
| created_at | TIMESTAMPTZ | 不可变 | 创建时间 |

### 5.5 API 详情

#### 5.5.1 创建用户

```
POST /api/platform-users
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "username": "operator01",    // 必填，非空
  "password": "securePass",    // 必填，非空
  "displayName": "运营一号",    // 可选
  "email": "op01@ezpos.net",   // 可选
  "enabled": true              // 可选，默认 true
}
```

**成功响应** (200):
```json
{
  "id": 1234567891,
  "username": "operator01",
  "displayName": "运营一号",
  "email": "op01@ezpos.net",
  "enabled": true
}
```

#### 5.5.2 用户列表（分页）

```
GET /api/platform-users?page=0&size=20
Authorization: Bearer {token}
```

**响应**: Spring Data 标准分页格式，`content` 为 `PlatformUserDto` 数组。

#### 5.5.3 用户详情

```
GET /api/platform-users/{id}
Authorization: Bearer {token}
```

**失败**: 用户不存在返回 404。

#### 5.5.4 更新用户

```
PATCH /api/platform-users/{id}
Authorization: Bearer {token}
```

**请求体** (所有字段可选):
```json
{
  "displayName": "新名称",
  "email": "new@ezpos.net",
  "enabled": false
}
```

#### 5.5.5 修改密码

```
PATCH /api/platform-users/{id}/password
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "oldPassword": "当前密码",    // 必填
  "newPassword": "新密码"       // 必填
}
```

**失败**: 旧密码不匹配返回 400。

---

## 6. 模块三：商家管理 (Merchant)

### 6.1 功能描述

管理入驻 EzPos 平台的商家信息，支持商家的创建、查询、修改和启用/停用操作。

### 6.2 功能清单

| 功能 | 说明 |
|------|------|
| 创建商家 | 录入新商家基本信息 |
| 商家列表 | 分页查询所有商家 |
| 商家详情 | 查询单个商家详情 |
| 更新商家 | 修改商家信息 |
| 启用商家 | 将商家状态设为启用 |
| 停用商家 | 将商家状态设为停用 |

### 6.3 业务规则

1. **默认启用**: 新创建的商家默认 `enabled = true`。
2. **部分更新**: PATCH 语义，仅传入字段被更新。
3. **启用/停用**: 通过独立的操作接口实现，不通过 PATCH 的 `enabled` 字段（虽然 PATCH 也支持）。
4. **不存在校验**: 查询、更新、启用/停用时如果商家不存在返回 404。

### 6.4 数据模型

**merchants 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | 商家 ID |
| name | VARCHAR(128) | NOT NULL | 商家名称 |
| contact_name | VARCHAR(64) | 可空 | 联系人姓名 |
| contact_phone | VARCHAR(32) | 可空 | 联系人电话 |
| address | VARCHAR(512) | 可空 | 商家地址 |
| memo | VARCHAR(1000) | 可空 | 备注 |
| enabled | BOOLEAN | DEFAULT true | 启用状态 |
| updated_at | TIMESTAMPTZ | 自动更新 | 最后修改时间 |
| created_at | TIMESTAMPTZ | 不可变 | 创建时间 |

### 6.5 API 详情

#### 6.5.1 创建商家

```
POST /api/merchants
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "name": "张记面馆",           // 必填
  "contactName": "张三",        // 可选
  "contactPhone": "13800138000", // 可选
  "address": "北京市朝阳区...",   // 可选
  "memo": "VIP 客户"            // 可选
}
```

#### 6.5.2 商家列表（分页）

```
GET /api/merchants?page=0&size=20
Authorization: Bearer {token}
```

#### 6.5.3 商家详情

```
GET /api/merchants/{id}
Authorization: Bearer {token}
```

#### 6.5.4 更新商家

```
PATCH /api/merchants/{id}
Authorization: Bearer {token}
```

**请求体** (所有字段可选):
```json
{
  "name": "新店名",
  "contactName": "李四",
  "contactPhone": "13900139000",
  "address": "新地址",
  "memo": "新备注"
}
```

#### 6.5.5 启用商家

```
POST /api/merchants/{id}:enable
Authorization: Bearer {token}
```

#### 6.5.6 停用商家

```
POST /api/merchants/{id}:disable
Authorization: Bearer {token}
```

---

## 7. 模块四：套餐与订阅管理 (Subscription & Plan)

### 7.1 功能描述

管理 SaaS 订阅套餐的定义与商家订阅关系。平台管理员可以创建套餐计划，为商家开通订阅，处理续费，并查询即将到期的订阅。

### 7.2 功能清单

**套餐管理 (Plan)**:

| 功能 | 说明 |
|------|------|
| 创建套餐 | 定义新的订阅套餐（名称、时长、价格） |
| 套餐列表 | 查询所有套餐 |
| 更新套餐 | 修改套餐信息 |

**订阅管理 (Subscription)**:

| 功能 | 说明 |
|------|------|
| 创建订阅 | 为商家开通订阅 |
| 订阅列表 | 分页查询所有订阅 |
| 续费 | 延长订阅有效期 |
| 到期预警 | 查询即将到期的订阅列表 |

### 7.3 业务规则

#### 套餐 (Plan)

1. **套餐定义**: 套餐包含名称、描述、有效天数、价格（单位：分）和启用状态。
2. **价格单位**: `price` 字段以**分**为单位存储（如 9999 表示 ¥99.99），避免浮点数精度问题。
3. **时长约束**: `durationDays` 必须为正整数。
4. **价格约束**: `price` 必须 ≥ 0。
5. **部分更新**: 套餐更新采用 PATCH 语义。

#### 订阅 (Subscription)

1. **创建订阅**:
   - 校验套餐存在。
   - `startDate` 自动设为**当天**。
   - `endDate` 自动计算为 `startDate + plan.durationDays` 天。
   - 初始状态为 `ACTIVE`。
2. **续费**:
   - 支持传入新的 `planId` 切换套餐，不传则沿用当前套餐。
   - 将 `endDate` 在当前值基础上延长对应套餐的 `durationDays` 天。
   - 记录 `renewedAt` 为当前时间。
3. **到期预警**: 查询 `status = ACTIVE` 且 `endDate < 当前日期 + N 天` 的订阅（默认 N = 30）。

### 7.4 数据模型

**plans 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | 套餐 ID |
| name | VARCHAR(128) | NOT NULL | 套餐名称（如"基础版"、"专业版"） |
| description | VARCHAR(512) | 可空 | 套餐描述 |
| duration_days | INTEGER | NOT NULL | 有效天数 |
| price | BIGINT | NOT NULL | 价格（单位：分） |
| enabled | BOOLEAN | DEFAULT true | 是否启用 |
| updated_at | TIMESTAMPTZ | 自动更新 | |
| created_at | TIMESTAMPTZ | 不可变 | |

**subscriptions 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | 订阅 ID |
| merchant_id | BIGINT | NOT NULL | 商家 ID |
| plan_id | BIGINT | NOT NULL | 套餐 ID |
| start_date | DATE | NOT NULL | 生效日期 |
| end_date | DATE | NOT NULL | 到期日期 |
| status | VARCHAR(16) | NOT NULL | 状态：ACTIVE / EXPIRED / CANCELLED |
| renewed_at | TIMESTAMPTZ | 可空 | 最近续费时间 |
| updated_at | TIMESTAMPTZ | 自动更新 | |
| created_at | TIMESTAMPTZ | 不可变 | |

**订阅状态枚举**:

| 状态 | 含义 |
|------|------|
| `ACTIVE` | 生效中 |
| `EXPIRED` | 已过期 |
| `CANCELLED` | 已取消 |

### 7.5 API 详情

#### 7.5.1 创建套餐

```
POST /api/plans
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "name": "专业版",          // 必填
  "description": "适合中小商家", // 可选
  "durationDays": 365,       // 必填，正整数
  "price": 99900,            // 必填，≥ 0（单位：分）
  "enabled": true            // 可选，默认 true
}
```

#### 7.5.2 套餐列表

```
GET /api/plans
Authorization: Bearer {token}
```

#### 7.5.3 更新套餐

```
PATCH /api/plans/{id}
Authorization: Bearer {token}
```

#### 7.5.4 创建订阅

```
POST /api/subscriptions
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "merchantId": 1234567890,   // 必填
  "planId": 9876543210        // 必填
}
```

**响应**: 包含自动计算的 `startDate` 和 `endDate`。

#### 7.5.5 订阅列表（分页）

```
GET /api/subscriptions?page=0&size=20
Authorization: Bearer {token}
```

#### 7.5.6 续费

```
POST /api/subscriptions/{id}:renew
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "planId": 9876543210   // 可选，不传则沿用当前套餐
}
```

#### 7.5.7 到期预警查询

```
GET /api/subscriptions/expiring?days=30
Authorization: Bearer {token}
```

**说明**: 返回未来 N 天内即将到期的 ACTIVE 状态订阅列表。

---

## 8. 模块五：版本发布与客户端更新 (Release)

### 8.1 功能描述

管理 EzPos 各客户端应用（POS 客户端、移动端、电子秤桥接程序等）的版本发布。支持灰度发布策略、强制更新机制、紧急暂停，以及客户端侧的无认证版本检查。

**这是系统中最复杂的模块**，包含三个子功能域：

1. **应用管理**: 注册和管理客户端应用标识
2. **发布管理**: 版本配置、灰度策略、发布/暂停生命周期
3. **客户端更新检查**: 面向客户端的公开 API，无需登录

### 8.2 功能清单

**应用管理 (Release Application)**:

| 功能 | 说明 |
|------|------|
| 创建应用 | 注册新的客户端应用（如 `ezpos-client`） |
| 应用列表 | 查询所有已注册应用 |
| 应用详情 | 按 code 查询应用信息 |
| 更新应用 | 修改应用名称、描述、启用状态 |

**发布管理 (Release)**:

| 功能 | 说明 |
|------|------|
| 创建发布 | 创建新版本发布配置 |
| 发布列表 | 按条件筛选查询发布列表 |
| 发布详情 | 查询单个发布配置 |
| 更新发布 | 修改版本配置信息 |
| 发布上线 | 将发布状态变更为已发布 |
| 暂停发布 | 紧急暂停正在分发的版本 |
| 恢复发布 | 从暂停状态恢复发布 |
| 完成制品上传 | 填写制品元数据（URL、哈希、大小） |
| 预签名上传 | 获取制品上传预签名 URL（**未实现，返回 501**） |

**客户端更新检查**:

| 功能 | 说明 |
|------|------|
| 检查更新 | 客户端检查是否有可用更新 |
| 上报更新状态 | 客户端上报更新执行结果 |

### 8.3 核心概念

#### 8.3.1 应用 (Application)

标识不同的客户端程序。每个应用通过唯一的 `code` 标识。

示例:
- `ezpos-client` — POS 桌面客户端
- `mobile-app` — 移动端 APP
- `scale-bridge` — 电子秤桥接程序

#### 8.3.2 发布 (Release)

一个 Release 是特定应用在特定平台上的**当前版本配置**。

**核心约束**: 每个 `(applicationCode, platform)` 组合**只能有一条** Release 记录（数据库唯一约束）。即同一应用的同一平台只维护一个"最新版本"配置。

#### 8.3.3 版本号格式

严格遵循语义化版本 (Semantic Versioning): `x.y.z`

- `x` — 主版本号
- `y` — 次版本号
- `z` — 修订号

示例: `1.0.0`, `2.3.1`, `10.0.12`

版本比较规则: 按 `major → minor → patch` 逐级比较。

### 8.4 发布生命周期

```
               ┌─── :publish ───┐
               ▼                │
  [创建] → PAUSED ────────► PUBLISHED
               ▲                │
               └─── :pause ─────┘
               │                │
               └─── :resume ────┘
```

| 状态 | 含义 | 说明 |
|------|------|------|
| `PAUSED` | 已暂停 | 创建后默认状态；客户端检查更新时不会返回该版本 |
| `PUBLISHED` | 已发布 | 活跃状态，客户端可以检查到并获取更新 |

- **创建后默认**: `PAUSED`（必须显式 `:publish` 才会对客户端可见）
- **首次发布**: 记录 `publishedAt` 时间戳
- **暂停后恢复**: `:resume` 操作，保留原始 `publishedAt`

### 8.5 灰度发布策略

系统支持三种灰度发布策略，通过 `rolloutType` 字段控制：

| 策略 | 说明 | 关键字段 |
|------|------|---------|
| `ALL` | 全量发布，所有符合条件的客户端都能收到更新 | 无额外字段 |
| `PERCENT` | 按比例灰度，基于租户/设备 ID 的稳定哈希分桶 | `percent` (0-100), `rolloutSalt` |
| `WHITELIST` | 白名单发布，仅指定租户可收到更新 | `whitelistTenants` |

#### PERCENT 策略详解

基于稳定哈希实现，确保同一租户/设备在多次检查中获得一致的结果（不会时而看到更新时而看不到）:

```
key = tenantId[:deviceId]:salt
bucket = unsigned(hashCode(key)) % 100
命中 = bucket < percent
```

- `salt` 优先使用 `rolloutSalt`，为空时使用 `version` 作为盐值
- 桶范围 0-99，`percent = 50` 表示约 50% 的客户端能收到更新
- 更换盐值会重新洗牌命中分布

#### WHITELIST 策略详解

- `whitelistTenants` 存储为逗号分隔的租户 ID 字符串
- 写入时自动去重、排序、去除首尾空格
- 检查时精确匹配 `tenantId`

### 8.6 强制更新机制

系统提供两种强制更新方式:

| 方式 | 字段 | 说明 |
|------|------|------|
| **最低版本强制** | `minSupportedVersion` | 低于此版本的客户端**必须更新**，忽略灰度规则 |
| **立即强制** | `isForced` | 设为 `true` 时，所有命中灰度的客户端被要求强制更新 |
| **定时强制** | `forceAfterAt` | 到达指定时间后，更新变为强制 |

**优先级**: `currentVersion < minSupportedVersion` 时，直接标记为强制更新，**跳过灰度判断逻辑**。

### 8.7 客户端更新检查流程

```
客户端请求 → 校验应用存在且启用
           → 查询 PUBLISHED 状态的 Release (按 applicationCode + platform)
           → 无 Release → 无更新
           → 解析版本号
           → currentVersion < minSupportedVersion → 强制更新（跳过灰度）
           → latestVersion <= currentVersion → 无更新
           → 执行灰度判断 (ALL/PERCENT/WHITELIST)
           → 未命中灰度 → 无更新
           → 命中灰度 → 判断是否强制
              → isForced = true → 强制更新
              → now >= forceAfterAt → 强制更新
              → 否则 → 可选更新
           → 返回更新信息
```

### 8.8 业务规则

#### 创建发布

1. 校验应用 (`applicationCode`) 存在且已启用。
2. 校验 `version` 和 `minSupportedVersion` 为合法的语义版本格式。
3. 校验 `minSupportedVersion <= version`。
4. 校验同一 `(applicationCode, platform)` 组合不存在已有记录（唯一约束）。
5. 校验灰度配置与 `rolloutType` 匹配。
6. 初始状态设为 `PAUSED`。

#### 发布上线 (:publish)

1. 校验 `artifactUrl` 或 `artifactKey` 至少有一个非空（确保客户端有下载地址）。
2. 校验版本号合法。
3. 校验灰度配置合法。
4. 首次发布记录 `publishedAt` 时间戳。
5. 将状态变更为 `PUBLISHED`。

#### 暂停 (:pause) 与恢复 (:resume)

- 暂停: 将状态设为 `PAUSED`，客户端将无法检查到此版本。
- 恢复: 等同于重新发布（需通过同样的前置校验），保留原始 `publishedAt`。

### 8.9 数据模型

**console_release_applications 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | |
| code | VARCHAR(64) | UNIQUE, NOT NULL | 应用标识码 |
| name | VARCHAR(128) | NOT NULL | 应用名称 |
| description | VARCHAR(512) | 可空 | 应用描述 |
| enabled | BOOLEAN | DEFAULT true | 是否启用 |
| updated_at | TIMESTAMPTZ | 自动更新 | |
| created_at | TIMESTAMPTZ | 不可变 | |

**console_releases 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | |
| application_code | VARCHAR(64) | NOT NULL | 关联应用 code |
| platform | VARCHAR(32) | NOT NULL | 平台标识 (如 windows, android, ios) |
| version | VARCHAR(32) | NOT NULL | 语义版本 x.y.z |
| min_supported_version | VARCHAR(32) | NOT NULL | 最低支持版本 |
| release_notes | VARCHAR(4000) | 可空 | 更新说明 |
| artifact_key | VARCHAR(512) | 可空 | 对象存储 Key |
| artifact_url | VARCHAR(2000) | 可空 | 下载地址 |
| sha256 | VARCHAR(128) | 可空 | 文件哈希 |
| file_size | BIGINT | 可空 | 文件大小（字节） |
| is_forced | BOOLEAN | DEFAULT false | 是否强制更新 |
| force_after_at | TIMESTAMPTZ | 可空 | 定时强制更新时间 |
| rollout_type | VARCHAR(16) | NOT NULL | 灰度策略: ALL / PERCENT / WHITELIST |
| percent | INTEGER | 可空 | 灰度百分比 (0-100) |
| whitelist_tenants | VARCHAR(8000) | 可空 | 白名单租户（逗号分隔） |
| rollout_salt | VARCHAR(64) | 可空 | 哈希盐值 |
| status | VARCHAR(16) | NOT NULL | 状态: PUBLISHED / PAUSED |
| published_at | TIMESTAMPTZ | 可空 | 首次发布时间 |
| updated_at | TIMESTAMPTZ | 自动更新 | |
| created_at | TIMESTAMPTZ | 不可变 | |

**UNIQUE 约束**: `(application_code, platform)`

**client_update_reports 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | |
| application_code | VARCHAR(64) | NOT NULL | 应用标识 |
| platform | VARCHAR(32) | NOT NULL | 平台 |
| tenant_id | VARCHAR(64) | 可空 | 租户 ID |
| device_id | VARCHAR(128) | 可空 | 设备 ID |
| from_version | VARCHAR(32) | 可空 | 更新前版本 |
| to_version | VARCHAR(32) | 可空 | 更新目标版本 |
| status | VARCHAR(16) | NOT NULL | 上报状态 |
| error_message | VARCHAR(1000) | 可空 | 失败信息 |
| created_at | TIMESTAMPTZ | 不可变 | |

### 8.10 API 详情

#### 应用管理

```
POST   /api/release-applications              # 创建应用
GET    /api/release-applications              # 应用列表（按 code 排序）
GET    /api/release-applications/{code}       # 应用详情
PATCH  /api/release-applications/{code}       # 更新应用
```

#### 发布管理

```
POST   /api/releases                          # 创建发布
GET    /api/releases?applicationCode=&platform=&status=  # 列表（支持筛选）
GET    /api/releases/{id}                     # 详情
PATCH  /api/releases/{id}                     # 更新
POST   /api/releases/{id}:publish             # 发布上线
POST   /api/releases/{id}:pause               # 暂停
POST   /api/releases/{id}:resume              # 恢复
POST   /api/releases/{id}/artifact:presign    # 预签名上传（未实现，501）
POST   /api/releases/{id}/artifact:complete   # 完成制品上传
```

#### 客户端更新检查（无需登录）

```
GET /api/client-updates/check
```

**请求头**:

| Header | 必填 | 说明 |
|--------|------|------|
| `X-App-Code` | 是 | 应用标识码 |
| `X-Platform` | 是 | 平台标识 |
| `X-Current-Version` | 是 | 当前版本号 (x.y.z) |
| `X-Tenant-Id` | 否 | 租户 ID（灰度判断需要） |
| `X-Device-Id` | 否 | 设备 ID（增强灰度精确度） |

**响应** (200) — 有更新:
```json
{
  "updateAvailable": true,
  "releaseId": 9876543210,
  "latestVersion": "2.1.0",
  "minSupportedVersion": "1.5.0",
  "isForced": true,
  "forceAfterAt": null,
  "releaseNotes": "修复若干 Bug，优化性能",
  "download": {
    "url": "https://cdn.ezpos.net/releases/ezpos-client-2.1.0.exe",
    "sha256": "abcdef1234567890...",
    "fileSize": 52428800
  }
}
```

**响应** (200) — 无更新:
```json
{
  "updateAvailable": false
}
```

#### 客户端上报更新状态

```
POST /api/client-updates/report
```

**请求体**:
```json
{
  "applicationCode": "ezpos-client",
  "platform": "windows",
  "tenantId": "tenant-001",
  "deviceId": "device-abc",
  "fromVersion": "1.0.0",
  "toVersion": "2.1.0",
  "status": "SUCCESS",
  "errorMessage": null
}
```

---

## 9. 模块六：数据迁移 (Migration)

### 9.1 功能描述

管理商家间的数据迁移任务。当需要将一个商家的数据（产品、分类、会员等）迁移到另一个商家时，创建迁移任务并跟踪执行进度。

> **注意**: 当前实现仅支持任务的**创建和查询**，实际迁移执行逻辑尚未实现。

### 9.2 功能清单

| 功能 | 说明 |
|------|------|
| 创建迁移任务 | 定义迁移的来源、目标和类型 |
| 任务列表 | 分页查询所有迁移任务 |
| 任务详情 | 查询单个迁移任务详情 |

### 9.3 业务规则

1. **创建**: 任务创建后初始状态为 `PENDING`，进度为 `0`。
2. **迁移类型**: 支持 `PRODUCT`（产品）、`CATEGORY`（分类）、`MEMBER`（会员）、`FULL`（全量）四种类型。
3. **任务不存在**: 查询不存在的任务返回 404。

### 9.4 数据模型

**data_migrations 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | |
| title | VARCHAR(128) | NOT NULL | 任务标题 |
| description | VARCHAR(1000) | 可空 | 任务描述 |
| source_merchant_id | BIGINT | 可空 | 源商家 ID |
| target_merchant_id | BIGINT | 可空 | 目标商家 ID |
| type | VARCHAR(32) | NOT NULL | 迁移类型 |
| status | VARCHAR(16) | NOT NULL | 任务状态 |
| progress | INTEGER | NOT NULL, DEFAULT 0 | 进度百分比 (0-100) |
| error_message | VARCHAR(2000) | 可空 | 错误信息 |
| started_at | TIMESTAMPTZ | 可空 | 开始执行时间 |
| completed_at | TIMESTAMPTZ | 可空 | 完成时间 |
| updated_at | TIMESTAMPTZ | 自动更新 | |
| created_at | TIMESTAMPTZ | 不可变 | |

**迁移类型枚举**:

| 类型 | 说明 |
|------|------|
| `PRODUCT` | 产品数据迁移 |
| `CATEGORY` | 分类数据迁移 |
| `MEMBER` | 会员数据迁移 |
| `FULL` | 全量数据迁移 |

**迁移状态枚举**:

| 状态 | 说明 |
|------|------|
| `PENDING` | 待执行 |
| `RUNNING` | 执行中 |
| `COMPLETED` | 已完成 |
| `FAILED` | 已失败 |

### 9.5 API 详情

```
POST /api/data-migrations                     # 创建迁移任务
GET  /api/data-migrations?page=0&size=20      # 任务列表（分页）
GET  /api/data-migrations/{id}                # 任务详情
```

**创建请求体**:
```json
{
  "title": "张记面馆数据迁移",          // 必填
  "description": "迁移全量产品和分类数据",  // 可选
  "sourceMerchantId": 1234567890,      // 可选
  "targetMerchantId": 9876543210,      // 可选
  "type": "FULL"                       // 必填
}
```

---

## 10. 模块七：审计日志 (Audit)

### 10.1 功能描述

记录平台上所有重要操作的审计日志，用于合规审计和问题追溯。

> **注意**: 当前实现仅支持日志的**分页查询**。日志的写入逻辑（拦截器或切面）尚未实现。

### 10.2 功能清单

| 功能 | 说明 |
|------|------|
| 审计日志列表 | 分页查询所有审计日志 |

### 10.3 数据模型

**audit_logs 表**:

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, Snowflake | |
| user_id | BIGINT | 可空 | 操作人 ID |
| username | VARCHAR(64) | 可空 | 操作人用户名（快照） |
| action | VARCHAR(64) | NOT NULL | 操作类型 |
| resource_type | VARCHAR(64) | 可空 | 资源类型 |
| resource_id | VARCHAR(128) | 可空 | 资源 ID |
| detail | VARCHAR(2000) | 可空 | 操作详情（JSON 或文本） |
| ip_address | VARCHAR(45) | 可空 | 操作者 IP 地址 |
| created_at | TIMESTAMPTZ | 不可变 | 操作时间 |

**操作类型示例**: `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `EXPORT`, `ENABLE`, `DISABLE`

**资源类型示例**: `User`, `Merchant`, `Plan`, `Subscription`, `Release`, `Application`

### 10.4 API 详情

```
GET /api/audit-logs?page=0&size=20
Authorization: Bearer {token}
```

---

## 11. 全局规范：错误响应

### 11.1 格式标准

所有错误响应统一使用 **RFC 9457 (Problem Details for HTTP APIs)** 格式，Content-Type 为 `application/problem+json`。

### 11.2 响应结构

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "PlatformUser not found: 123",
  "instance": "/api/platform-users/123"
}
```

| 字段 | 说明 |
|------|------|
| `type` | 问题类型 URI（当前统一为 `about:blank`） |
| `title` | HTTP 状态描述（如 `Bad Request`, `Unauthorized`） |
| `status` | HTTP 状态码 |
| `detail` | 详细错误说明 |
| `instance` | 发生错误的请求路径 |

### 11.3 验证错误扩展

当请求参数验证失败时，额外包含 `errors` 字段:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/platform-users",
  "errors": [
    { "field": "username", "message": "must not be blank" },
    { "field": "password", "message": "must not be blank" }
  ]
}
```

### 11.4 业务异常映射

| 异常类 | HTTP 状态码 | 场景 |
|--------|-----------|------|
| `EntityNotFoundException` | 404 | 查询的资源不存在 |
| `EntityAlreadyExistsException` | 409 | 创建时资源已存在（如用户名重复） |
| `BusinessRuleException` | 400 | 业务规则校验失败 |
| `AuthenticationFailedException` | 401 | 认证失败（用户名/密码错误、账号禁用） |
| `DataIntegrityException` | 500 | 数据完整性异常 |
| `ResponseStatusException` | 动态 | 通用 HTTP 状态异常 |
| 参数验证失败 | 400 | `@Valid` 注解校验不通过 |
| `AccessDeniedException` | 403 | 已认证但权限不足 |
| 未知异常 | 500 | 服务器内部错误 |

### 11.5 设计约定

- **仅错误用 ProblemDetail**: 成功响应（2xx）直接返回业务 DTO，不做任何包装。
- **安全响应统一化**: 401 和 403 通过 `ProblemDetailAuthenticationEntryPoint` 和 `ProblemDetailAccessDeniedHandler` 统一输出。

---

## 12. 全局规范：认证机制

### 12.1 认证架构

```
客户端请求
  │
  ▼
Authorization: Bearer {opaque-token}
  │
  ▼
OpaqueTokenAuthenticationFilter
  │
  ├── 提取 Bearer Token
  ├── TokenIntrospector.resolveUserId(token)  ──► Redis 查询
  ├── PrincipalLoader.load(userId)            ──► 数据库查询
  └── 写入 SecurityContext
  │
  ▼
业务 Controller
```

### 12.2 公开端点（无需认证）

| 路径 | 说明 |
|------|------|
| `OPTIONS /**` | CORS 预检请求 |
| `/api/auth/login` | 登录接口 |
| `/api/client-updates/**` | 客户端更新检查与上报 |
| `/v3/api-docs/**` | OpenAPI 文档 |
| `/swagger-ui/**` | Swagger UI |
| `/swagger-ui.html` | Swagger 入口 |
| `/actuator/**` | 健康检查与监控 |

### 12.3 SPI 扩展点

| 接口 | 当前实现 | 可替换为 |
|------|---------|---------|
| `TokenIntrospector` | `OpaqueTokenService`（Redis） | JWT 解析、远程令牌验证 |
| `PrincipalLoader` | `UserPrincipalLoader`（数据库） | 缓存加载、外部用户中心 |

### 12.4 获取当前用户

业务代码推荐通过注入 `CurrentPrincipalProvider` 获取当前登录用户:

```kotlin
// 获取当前用户 ID（未认证则抛 401）
val userId = currentPrincipalProvider.requireUserId()

// 尝试获取（可能为 null）
val principal = currentPrincipalProvider.principalOrNull()
```

---

## 13. 非功能性需求

### 13.1 安全性

| 需求 | 实现 |
|------|------|
| 密码存储 | BCrypt 加密，不存储明文 |
| 令牌安全 | 32 字节密码学随机数，Base64 URL 安全编码 |
| 令牌过期 | 滑动过期 24 小时 |
| 无状态认证 | 不使用 HttpSession，不使用 Cookie |
| CSRF 防护 | 已禁用（API-only 服务，使用 Token 认证） |
| 错误信息隐藏 | 认证失败统一返回相同错误信息，不区分"用户不存在"和"密码错误" |

### 13.2 ID 生成

采用 **雪花算法 (Snowflake ID)** 生成全局唯一 ID:

| 参数 | 配置 | 说明 |
|------|------|------|
| 时间戳位 | MSB | 毫秒级 |
| 机器 ID 位 | 5 bit | 支持 0-31 台机器 |
| 序列号位 | 8 bit | 每毫秒支持 0-255 个 ID |
| 纪元 | 2025-01-01 | 自定义时间起点 |
| 时钟回拨处理 | 强制前进 1ms | 防止 ID 冲突 |

### 13.3 数据库

| 需求 | 说明 |
|------|------|
| 数据库 | PostgreSQL |
| 迁移工具 | Flyway |
| Schema 管理 | Flyway 为唯一 Schema 来源（生产环境 `ddl-auto: validate`） |
| 时间字段 | 统一使用 `TIMESTAMPTZ`（带时区） |
| 主键 | 统一使用 `BIGINT` + Snowflake ID |

### 13.4 可观测性

| 需求 | 实现 |
|------|------|
| 健康检查 | Spring Actuator `/actuator/health` |
| API 文档 | Swagger UI `/swagger-ui/index.html` |
| SQL 日志 | 开发环境开启 Hibernate SQL + 参数绑定日志 |

### 13.5 环境配置

| 环境 | 特点 |
|------|------|
| **dev** (默认) | `ddl-auto: update`、Debug SQL 日志、本地 PG/Redis |
| **prod** | `ddl-auto: validate`、Warn 日志级别、环境变量注入数据库连接 |

---

## 14. 数据模型总览

### 14.1 ER 关系

```
platform_users
    │
    │ (认证/登录)
    ▼
opaque-token (Redis)

merchants ─────────────┐
    │                   │
    │ (merchant_id)     │ (source/target_merchant_id)
    ▼                   ▼
subscriptions      data_migrations
    │
    │ (plan_id)
    ▼
plans

console_release_applications
    │
    │ (application_code)
    ▼
console_releases
    │
    │ (application_code + platform)
    ▼
client_update_reports

audit_logs (独立，记录所有操作)
```

### 14.2 表清单

| 表名 | 说明 | 记录量级 |
|------|------|---------|
| `platform_users` | 平台用户 | 小（数十） |
| `merchants` | 商家 | 中（数百至数千） |
| `plans` | 订阅套餐 | 小（个位数） |
| `subscriptions` | 商家订阅 | 中（与商家数量对应） |
| `console_release_applications` | 客户端应用 | 小（个位数） |
| `console_releases` | 版本发布配置 | 小（每应用每平台一条） |
| `client_update_reports` | 客户端更新上报 | 大（持续增长） |
| `data_migrations` | 数据迁移任务 | 小 |
| `audit_logs` | 审计日志 | 大（持续增长） |

---

## 15. API 清单汇总

### 15.1 需认证接口

| 方法 | 路径 | 模块 | 说明 |
|------|------|------|------|
| `GET` | `/api/auth/me` | Auth | 获取当前用户 |
| `POST` | `/api/auth/logout` | Auth | 登出 |
| `POST` | `/api/platform-users` | User | 创建用户 |
| `GET` | `/api/platform-users` | User | 用户列表 |
| `GET` | `/api/platform-users/{id}` | User | 用户详情 |
| `PATCH` | `/api/platform-users/{id}` | User | 更新用户 |
| `PATCH` | `/api/platform-users/{id}/password` | User | 修改密码 |
| `POST` | `/api/merchants` | Merchant | 创建商家 |
| `GET` | `/api/merchants` | Merchant | 商家列表 |
| `GET` | `/api/merchants/{id}` | Merchant | 商家详情 |
| `PATCH` | `/api/merchants/{id}` | Merchant | 更新商家 |
| `POST` | `/api/merchants/{id}:enable` | Merchant | 启用商家 |
| `POST` | `/api/merchants/{id}:disable` | Merchant | 停用商家 |
| `POST` | `/api/plans` | Plan | 创建套餐 |
| `GET` | `/api/plans` | Plan | 套餐列表 |
| `PATCH` | `/api/plans/{id}` | Plan | 更新套餐 |
| `POST` | `/api/subscriptions` | Subscription | 创建订阅 |
| `GET` | `/api/subscriptions` | Subscription | 订阅列表 |
| `POST` | `/api/subscriptions/{id}:renew` | Subscription | 续费 |
| `GET` | `/api/subscriptions/expiring` | Subscription | 到期预警 |
| `POST` | `/api/release-applications` | Release | 创建应用 |
| `GET` | `/api/release-applications` | Release | 应用列表 |
| `GET` | `/api/release-applications/{code}` | Release | 应用详情 |
| `PATCH` | `/api/release-applications/{code}` | Release | 更新应用 |
| `POST` | `/api/releases` | Release | 创建发布 |
| `GET` | `/api/releases` | Release | 发布列表 |
| `GET` | `/api/releases/{id}` | Release | 发布详情 |
| `PATCH` | `/api/releases/{id}` | Release | 更新发布 |
| `POST` | `/api/releases/{id}:publish` | Release | 发布上线 |
| `POST` | `/api/releases/{id}:pause` | Release | 暂停发布 |
| `POST` | `/api/releases/{id}:resume` | Release | 恢复发布 |
| `POST` | `/api/releases/{id}/artifact:presign` | Release | 预签名（501） |
| `POST` | `/api/releases/{id}/artifact:complete` | Release | 完成制品 |
| `POST` | `/api/data-migrations` | Migration | 创建迁移 |
| `GET` | `/api/data-migrations` | Migration | 迁移列表 |
| `GET` | `/api/data-migrations/{id}` | Migration | 迁移详情 |
| `GET` | `/api/audit-logs` | Audit | 审计日志 |

### 15.2 公开接口（无需认证）

| 方法 | 路径 | 模块 | 说明 |
|------|------|------|------|
| `POST` | `/api/auth/login` | Auth | 登录 |
| `GET` | `/api/client-updates/check` | Release | 客户端检查更新 |
| `POST` | `/api/client-updates/report` | Release | 客户端上报状态 |
| `GET` | `/v3/api-docs/**` | Infra | OpenAPI 文档 |
| `GET` | `/swagger-ui/**` | Infra | Swagger UI |
| `GET` | `/actuator/**` | Infra | 健康检查 |

---

## 16. TODO：尚未实现的功能清单

以下功能在代码中已预留框架、接口桩或数据模型，但**业务逻辑尚未完整实现**，需要后续开发补齐。

### 16.1 高优先级

| # | 模块 | 功能 | 当前状态 | 待实现内容 |
|---|------|------|---------|-----------|
| 1 | Auth | RBAC 角色权限控制 | `PlatformUserPrincipal.authorities` 返回空列表 | 设计角色模型（如 SUPER_ADMIN / OPERATOR / VIEWER），实现角色表、用户-角色关联表，在 `PrincipalLoader` 中加载权限，Controller 添加 `@PreAuthorize` 注解 |
| 2 | Audit | 审计日志自动写入 | 仅有查询接口和数据表，无写入逻辑 | 实现 AOP 切面或拦截器，在关键业务操作（登录/登出、CRUD、启用/停用等）时自动记录审计日志，包括操作人、操作类型、资源类型/ID、变更详情、IP 地址 |
| 3 | Migration | 数据迁移执行引擎 | 仅支持创建任务和查询，无实际执行逻辑 | 实现异步迁移执行器，支持按类型（PRODUCT/CATEGORY/MEMBER/FULL）从源商家复制数据到目标商家，更新任务进度和状态（PENDING → RUNNING → COMPLETED/FAILED） |
| 4 | Release | 制品预签名上传 | `POST /api/releases/{id}/artifact:presign` 返回 501 | 对接对象存储（如 AWS S3 / MinIO / 阿里云 OSS），生成预签名上传 URL，客户端直传制品文件 |

### 16.2 中优先级

| # | 模块 | 功能 | 说明 |
|---|------|------|------|
| 5 | Subscription | 订阅自动过期 | 当前无定时任务将已过 `endDate` 的 ACTIVE 订阅自动标记为 EXPIRED，需实现定时调度（如 `@Scheduled`） |
| 6 | Subscription | 订阅取消 | 当前无取消订阅接口，需添加 `POST /api/subscriptions/{id}:cancel` 将状态变更为 CANCELLED |
| 7 | Merchant | 商家与订阅关联校验 | 创建订阅时未校验 `merchantId` 是否存在且启用，需添加跨模块校验 |
| 8 | Merchant | 商家搜索与筛选 | 当前商家列表仅支持分页，不支持按名称、启用状态、联系人等条件筛选 |
| 9 | User | 用户搜索与筛选 | 当前用户列表仅支持分页，不支持按用户名、启用状态等条件筛选 |
| 10 | Audit | 审计日志筛选 | 当前审计日志列表仅支持分页，不支持按操作人、操作类型、资源类型、时间范围等条件筛选 |
| 11 | Release | 发布历史/版本归档 | 当前每个 (applicationCode, platform) 只保留一条 Release 记录，无历史版本归档机制 |

### 16.3 低优先级 / 增强项

| # | 模块 | 功能 | 说明 |
|---|------|------|------|
| 12 | Auth | Token 多端管理 | 当前用户可同时持有多个有效 Token（多端登录），无"踢出其他设备"或"查看登录设备列表"功能 |
| 13 | Auth | 登录安全增强 | 缺少登录失败次数限制、账号锁定、验证码等暴力破解防护 |
| 14 | Auth | 密码策略 | 无密码复杂度要求（长度、大小写、特殊字符）、密码过期策略、历史密码检查 |
| 15 | User | 用户删除/软删除 | 当前无删除用户功能，仅能通过 `enabled = false` 禁用 |
| 16 | Merchant | 商家删除/软删除 | 当前无删除商家功能，仅能通过停用实现 |
| 17 | Subscription | 到期提醒通知 | `GET /api/subscriptions/expiring` 仅供手动查询，无自动邮件/消息通知机制 |
| 18 | Release | 客户端更新统计面板 | `client_update_reports` 表持续收集数据，但无统计查询接口（如：按版本统计更新成功率、更新趋势等） |
| 19 | Migration | 迁移任务取消/重试 | 无取消正在执行的迁移任务或重试失败任务的功能 |
| 20 | 全局 | CORS 跨域配置 | SecurityConfig 仅放行了 `OPTIONS` 预检请求，未配置具体的允许源、允许头等 CORS 策略 |
| 21 | 全局 | 请求日志/链路追踪 | `common/observability` 包预留但尚未实现，缺少请求 ID 注入、链路追踪（如 Micrometer Tracing） |
| 22 | 全局 | 分页参数校验 | 列表接口未限制最大 `size`，可能被传入极大值导致性能问题 |
| 23 | 全局 | API 限流 | 无接口级别的限流保护（Rate Limiting），尤其是客户端更新检查等公开接口 |
| 24 | 全局 | 国际化 (i18n) | 错误信息和验证消息目前为硬编码英文/中文混合，无多语言支持 |

---

> **文档说明**: 本 PRD 基于截至 2026-03-01 的代码实现梳理，反映系统**当前已实现**的功能。标注为"未实现"的功能（如制品预签名上传、审计日志写入、数据迁移执行）为代码中预留的框架或接口桩。
