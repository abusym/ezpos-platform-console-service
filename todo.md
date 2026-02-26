# 接口 TODO

> 基于 README.md 与 docs/ 文档梳理的全量接口清单。
>
> 状态说明：✅ 已实现 | 🚧 已实现但有限制 | ❌ 未实现

---

## 1. 认证模块（Auth）

> 文档：`docs/auth/README.md`
> 鉴权方式：Opaque Token + Redis，滑动过期 24h

| 方法 | 路径 | 说明 | 鉴权 | 状态 |
|------|------|------|------|------|
| POST | `/api/auth/login` | 登录，返回 accessToken | 无 | ✅ |
| GET | `/api/auth/me` | 获取当前登录用户信息 | Bearer | ✅ |
| POST | `/api/auth/logout` | 登出，撤销当前 token | Bearer | ✅ |

### 待扩展

| 说明 | 优先级 | 备注 |
|------|--------|------|
| RBAC 权限体系 | 中 | `PlatformUserPrincipal.authorities` 目前为空，需设计角色/权限模型 |
| JWT 支持 | 低 | 替换 `TokenIntrospector` 实现即可，无需改 Filter |

---

## 2. 平台用户管理（Platform Users）

> 代码：`feature/user`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/platform-users` | 创建用户 | ✅ |
| GET | `/api/platform-users` | 用户列表（分页） | ✅ |
| GET | `/api/platform-users/{id}` | 用户详情 | ✅ |
| PATCH | `/api/platform-users/{id}` | 更新用户（displayName / email / enabled） | ✅ |
| PATCH | `/api/platform-users/{id}/password` | 修改密码（需验证旧密码） | ✅ |

---

## 3. 发布应用管理（Release Applications）

> 文档：`docs/modules/release/README.md`
> 代码：`feature/release/controller/ReleaseApplicationsController`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/release-applications` | 应用列表 | ✅ |
| POST | `/api/release-applications` | 创建应用 | ✅ |
| GET | `/api/release-applications/{code}` | 按 code 查详情 | ✅ |
| PATCH | `/api/release-applications/{code}` | 更新应用（name / description / enabled） | ✅ |

---

## 4. 发布管理（Releases）

> 文档：`docs/modules/release/README.md`
> 代码：`feature/release/controller/ReleasesController`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/releases?applicationCode=&platform=&status=&page=&size=&sort=` | 分页查询 | ✅ |
| GET | `/api/releases/{id}` | 详情 | ✅ |
| POST | `/api/releases` | 创建（同 app+platform 409） | ✅ |
| PATCH | `/api/releases/{id}` | 局部更新 | ✅ |
| POST | `/api/releases/{id}:publish` | 发布 | ✅ |
| POST | `/api/releases/{id}:pause` | 暂停（止损） | ✅ |
| POST | `/api/releases/{id}:resume` | 恢复发布 | ✅ |
| POST | `/api/releases/{id}/artifact:presign` | 预签名上传 | 🚧 返回 501，需接入对象存储 |
| POST | `/api/releases/{id}/artifact:complete` | 补全制品信息 | ✅ |

---

## 5. 客户端更新（Client Updates）

> 文档：`docs/modules/release/README.md`
> 代码：`feature/release/controller/ClientUpdatesController`
> **无需登录**（SecurityConfig 放行 `/api/client-updates/**`）

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/client-updates/check` | 检查更新（Header 传参） | ✅ |
| POST | `/api/client-updates/report` | 上报更新状态（downloaded / installed / failed） | ✅ |

请求头约定（check 接口）：

| Header | 必填 | 说明 |
|--------|------|------|
| `X-App-Code` | 是 | 应用编码 |
| `X-Platform` | 是 | 平台 |
| `X-Current-Version` | 是 | 当前版本（`x.y.z`） |
| `X-Tenant-Id` | 是 | 租户 id |
| `X-Device-Id` | 否 | 设备 id（百分比灰度打散） |

---

## 6. 商家管理（Merchant）

> 代码：`feature/merchant`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/merchants` | 商家入驻 | ✅ |
| GET | `/api/merchants` | 商家列表（分页） | ✅ |
| GET | `/api/merchants/{id}` | 商家详情 | ✅ |
| PATCH | `/api/merchants/{id}` | 资料维护 | ✅ |
| POST | `/api/merchants/{id}:enable` | 启用商家 | ✅ |
| POST | `/api/merchants/{id}:disable` | 停用商家 | ✅ |

---

## 7. 续费与订阅（Subscription）

> 代码：`feature/subscription`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/plans` | 创建套餐 | ✅ |
| GET | `/api/plans` | 套餐列表 | ✅ |
| PATCH | `/api/plans/{id}` | 更新套餐 | ✅ |
| GET | `/api/subscriptions` | 订阅列表（分页） | ✅ |
| POST | `/api/subscriptions` | 创建订阅 | ✅ |
| POST | `/api/subscriptions/{id}:renew` | 续费 | ✅ |
| GET | `/api/subscriptions/expiring` | 到期提醒列表（默认 30 天内） | ✅ |

---

## 8. 数据迁移（Migration）

> 代码：`feature/migration`
> 所有接口需登录

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/data-migrations` | 创建迁移任务 | ✅ |
| GET | `/api/data-migrations` | 迁移任务列表（分页） | ✅ |
| GET | `/api/data-migrations/{id}` | 迁移详情 / 进度 | ✅ |

---

## 9. 平台运维（Operations）

> 代码：`feature/audit`（审计日志）+ Actuator（健康检查）

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/audit-logs` | 审计日志查询（分页） | ✅ |
| GET | `/actuator/health` | 健康检查 | ✅ |

---

## 汇总

| 模块 | 已实现 | 受限 | 未实现 |
|------|--------|------|--------|
| 认证（Auth） | 3 | — | — |
| 平台用户（Users） | 5 | — | — |
| 发布应用（Release Apps） | 4 | — | — |
| 发布管理（Releases） | 8 | 1（presign） | — |
| 客户端更新（Client Updates） | 2 | — | — |
| 商家管理（Merchant） | 6 | — | — |
| 续费与订阅（Subscription） | 7 | — | — |
| 数据迁移（Migration） | 3 | — | — |
| 平台运维（Operations） | 2 | — | — |
| **合计** | **40** | **1** | **0** |

---

## 备注：README.md 与实际配置不一致

README 中的示例 `application-dev.yaml` 与仓库实际文件不同：

| 配置项 | README 示例 | 实际 `application-dev.yaml` |
|--------|-------------|----------------------------|
| `username` | `postgres` | `root` |
| `password` | `123456` | `root` |
| `ddl-auto` | `update` | `update` |
| `logging.level.org.hibernate.SQL` | `WARN` | `DEBUG` |

建议同步 README 或标注"仅为示例"。
