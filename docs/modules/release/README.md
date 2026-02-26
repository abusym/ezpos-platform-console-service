# Release（客户端版本发布 / 升级中心）模块说明

> 本模块隶属于 `ezpos-platform-console-service`（平台总后台），用于提供 **客户端应用的版本管理、发布（Release）、灰度发布、强制升级、暂停止损** 等能力。
>
> **本文档以当前仓库代码为准**（`net.ezpos.console.feature.release`）。若文档与实现不一致，请优先以代码与接口行为为准。

## 目录

- [背景与定位](#背景与定位)
- [模块职责范围（In / Out）](#模块职责范围in--out)
- [在当前项目中的落点（代码与文档）](#在当前项目中的落点代码与文档)
- [依赖与复用（作为模块的关键点）](#依赖与复用作为模块的关键点)
- [核心概念模型](#核心概念模型)
- [API 设计（Admin / Client）](#api-设计admin--client)
- [客户端更新检查流程（Client Update API）](#客户端更新检查流程client-update-api)
- [灰度发布策略（Rollout Engine）](#灰度发布策略rollout-engine)
- [安全机制（下载与完整性）](#安全机制下载与完整性)
- [升级统计与监控](#升级统计与监控)
- [运行与部署（模块形态）](#运行与部署模块形态)
- [演进方向](#演进方向)
- [目标输出（落地优先级）](#目标输出落地优先级)

## 背景与定位

EzPos 是多终端收银 SaaS 系统，包含多个客户端程序（示例）：

- 收银前台（Windows 桌面）
- 标签打印工具（Windows 桌面）
- 数据导入工具（Windows 桌面）
- 电子秤传秤工具（Windows 桌面）
- 监控大屏工具（Windows 桌面）
- 手机端 App（Android / iOS）

平台侧需要统一能力来完成：

- “平台侧”统一管理各客户端版本与发布策略（灰度/白名单/强更/暂停止损）
- “客户端侧”通过统一 API 检查更新并获取下载信息


## 模块职责范围（In / Out）

### In Scope（本模块负责）

- Application（客户端应用）管理
- Release（发布）管理：灰度策略、白名单、强更策略、发布状态（published/paused）
- Client Update API：检查更新
- 产物信息维护：在 Release 上维护 `artifactUrl/artifactKey/sha256/fileSize` 等字段

### Out of Scope（不在本模块）

- 客户端安装器/自更新程序的实现细节（属于各客户端工程）
- 代码签名证书的签发与管理流程（本模块只存放校验/签名结果字段）
- 对象存储预签名（Presign）能力接入（当前 `artifact:presign` 返回 501，见下文 API）

## 在当前项目中的落点（代码与文档）

### 包与目录（建议）

- 模块根包：`net.ezpos.console.feature.release`
- 建议目录：`src/main/kotlin/net/ezpos/console/feature/release/{controller,dto,service,entity,repository,mapper}`

## 依赖与复用（作为模块的关键点）

- 认证鉴权（Admin API）：复用 Console 现有 **Bearer Opaque Token + Redis** 体系
- 错误响应：复用全局 `ProblemDetail (application/problem+json)` 输出规范
- ID：复用现有 Snowflake（如实体基类/ID 生成策略）
- 数据访问：复用 Spring Data JPA + PostgreSQL
- 对象存储：Release 记录上保留 `artifactKey/artifactUrl` 等字段；**预签名（presign）链路尚未接入**（`artifact:presign` 返回 501）

## 核心概念模型

### Application（客户端应用）

用于标识“哪个客户端程序”的升级体系。

**示例（code）**

- `ezpos-client`
- `ezpos-merchant-data-importer`
- `scale-bridge`
- `wallboard`
- `mobile-app`

**字段（实际对外 DTO：`ReleaseApplicationDto`）**

| 字段 | 说明 |
| --- | --- |
| `id` | 记录 id |
| `code` | 应用唯一标识（与客户端上报一致） |
| `name` | 应用名称 |
| `description` | 描述 |
| `enabled` | 是否启用 |

### Release（发布）

Release 表示某个应用在某个平台上的“当前发布配置”，并绑定灰度/白名单与强更策略。

> 代码约束：数据库层对 `(applicationCode, platform)` 做了唯一约束，因此 **同一应用同一平台只维护一条 Release 记录**（通过 `PATCH` 进行更新）。

> 本项目当前规模下，建议直接把“产物元数据（artifactUrl/sha256/fileSize）”放在 Release 上，省去 Build 这一层。

**字段（实际对外 DTO：`ReleaseDto`）**

| 字段 | 说明 |
| --- | --- |
| `id` | 发布记录 id |
| `applicationCode` | 所属应用（关联 Application.code） |
| `platform` | 平台（字符串；由调用方约定，如 `windows-x64` / `android` / `ios`） |
| `version` | 发布版本（**严格 `x.y.z` 三段式**，例如 `1.8.3`） |
| `releaseNotes` | 更新说明 |
| `artifactKey` | 对象存储 key（或 `artifactUrl` 指针） |
| `artifactUrl` | 下载地址（当前客户端检查更新 **仅使用该字段**；仅配置 `artifactKey` 会导致客户端检查接口 500） |
| `sha256` | 文件校验（SHA256，建议保留） |
| `fileSize` | 文件大小（建议保留） |
| `minSupportedVersion` | 最低支持版本（必填，见下文） |
| `isForced` | 是否强制升级 |
| `forceAfterAt` | 定时强制升级时间（可选） |
| `rolloutType` | 灰度类型（`all` / `percent` / `whitelist`） |
| `percent` | 灰度百分比（rolloutType=percent 时使用，0-100） |
| `whitelistTenants` | 租户白名单（rolloutType=whitelist 时使用） |
| `rolloutSalt` | 稳定分流盐（可选；为空则默认使用 `version`） |
| `status` | 发布状态（`published` / `paused`，见下文） |
| `publishedAt` | 发布时间 |
| `createdAt` | 创建时间 |
| `updatedAt` | 更新时间 |

### 最低支持版本（`minSupportedVersion`）

`minSupportedVersion` 用于定义“**低于该版本的客户端不再被支持**”，常用于不可跳过的重大升级（例如协议不兼容、数据结构变更、安全漏洞修复等）。

**规则（建议）**

- 若 `currentVersion < minSupportedVersion`：客户端必须升级。
  - 代码实现：服务端 `check` 返回 `updateAvailable=true` 且 `isForced=true`（**忽略灰度/白名单**）。
- 若 `currentVersion >= minSupportedVersion`：是否升级取决于版本比较与灰度/白名单命中。

> 说明：`minSupportedVersion` 与 `isForced/forceAfterAt` 不冲突。
>
> - `minSupportedVersion` 解决的是“**版本下限**”（太旧的版本不能继续用）。
> - `isForced/forceAfterAt` 解决的是“**发布策略**”（即使版本不旧，也要求用户在某时间点前升级）。

### 发布状态（`status`）

本模块采用**极简发布状态**：只保留 `published/paused` 两个状态，用于紧急止损与显式控制“是否对客户端生效”。

**状态定义**

- `published`：对客户端生效。客户端 `check` 会基于该 Release 做版本比较与灰度/白名单判定。
- `paused`：暂停发布（止损）。客户端 `check` 必须忽略该 Release（视为无可用更新）。

**状态切换规则（建议）**

- `publish/resume`：把 `status` 置为 `published`，并在首次发布时写入 `publishedAt=now()`（后续 resume 可不改 publishedAt，按你偏好）。
- `pause`：把 `status` 置为 `paused`。

**客户端 `check` 的筛选规则**

- 只查询 `status=published` 的 Release。
- 若不存在 `published` Release，则返回 `updateAvailable=false`。

## API 设计（Admin / Client）

本模块的接口分为两类：

- **Admin API**：平台侧管理端使用（创建应用、配置 Release、上传产物、灰度/白名单、强更、暂停止损），需要登录鉴权。
- **Client API**：客户端/设备用于检查更新（稳定、轻量、向后兼容）。

### Admin API（平台侧管理端，需登录）

建议按资源组织，统一前缀为 `/api`（与项目现有接口一致）。

#### Application

- `GET /api/release-applications`
- `POST /api/release-applications`
- `GET /api/release-applications/{code}`
- `PATCH /api/release-applications/{code}`

#### Release

- `GET /api/releases?applicationCode=&platform=&status=&page=&size=&sort=`（分页；返回 Page，支持排序；未传 sort 时默认按 `updatedAt desc, applicationCode asc, platform asc`）
- `GET /api/releases/{id}`
- `POST /api/releases`：**创建（create-only）**；同一 `(applicationCode, platform)` 已存在会返回 409
- `PATCH /api/releases/{id}`：局部更新（版本/灰度/制品/强更等）
- `POST /api/releases/{id}:publish`：发布（会做“可发布”前置校验）
- `POST /api/releases/{id}:pause`：暂停
- `POST /api/releases/{id}:resume`：恢复（同 publish 的前置校验）

产物上传（推荐两步，避免 Console Service 大文件中转）：

- `POST /api/releases/{id}/artifact:presign`：**当前返回 501（未接入对象存储预签名）**
- `POST /api/releases/{id}/artifact:complete`：写入/更新 `artifactKey/artifactUrl/sha256/fileSize`

> 约定（代码实现）：新建 Release 默认 `status=paused`，需要显式调用 `:publish` / `:resume` 才会进入 `published` 并对客户端生效。
>
> `:publish/:resume` 会校验“可发布”前置条件（至少需要 `artifactUrl` 或 `artifactKey` 之一；版本号与灰度字段组合也必须合法）。

## 客户端更新检查流程（Client Update API）

### 接口

`GET /api/client-updates/check`

该接口 **无需登录**（在 Spring Security 中对 `/api/client-updates/**` 放行）。

### 请求头（代码实现）

该接口不使用 query 参数，全部通过 Header 传参：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-App-Code` | 是 | 应用编码 |
| `X-Platform` | 是 | 平台 |
| `X-Current-Version` | 是 | 当前版本（严格 `x.y.z`） |
| `X-Tenant-Id` | 是 | 租户 id |
| `X-Device-Id` | 否 | 设备 id（仅用于百分比灰度进一步打散） |

### 服务端逻辑（代码实现）

1. 校验应用存在且启用：否则返回 404
2. 按 `applicationCode + platform + status=published` 查询 Release：不存在则返回无更新
3. 校验 `currentVersion` 合法（严格 `x.y.z`）：否则 400
4. 解析 `release.version` 与 `release.minSupportedVersion`（均要求 `x.y.z`）：解析失败则 500（数据异常）
5. 要求 `release.artifactUrl` 已配置：否则 500（当前实现仅用 URL）
6. 若 `currentVersion < minSupportedVersion`：直接返回强制更新（`isForced=true`，忽略灰度规则）
7. 若 `latestVersion <= currentVersion`：返回无更新
8. 灰度判定（all/percent/whitelist）：未命中则无更新；命中则返回更新信息（含 `isForced/forceAfterAt`）

### 返回示例（与 `ClientUpdateCheckResponse` 一致）

```json
{
  "updateAvailable": true,
  "releaseId": 10001,
  "latestVersion": "1.8.3",
  "minSupportedVersion": "1.6.0",
  "isForced": false,
  "forceAfterAt": null,
  "releaseNotes": "修复称重异常；优化同步性能",
  "download": {
    "url": "https://cdn.ezpos.net/cashier/1.8.3/windows-x64/app.zip",
    "sha256": "xxxx",
    "fileSize": 12345678
  }
}
```

## 灰度发布策略（Rollout Engine）

灰度采用稳定散列分桶，确保同一租户（可选区分设备）在同一 salt 下的命中结果稳定。

### 全量（`rolloutType=all`）

- 恒命中（只要版本比较等前置条件满足）

### 百分比灰度（`rolloutType=percent`）

```text
bucket = (unsigned(hashcode(key)) % 100)
bucket < percent
```

其中：

- `key = tenantId[:deviceId]:salt`
- `salt = rolloutSalt`（非空优先）否则为 `release.version`
- `hashcode()` 使用 JVM 的 `String.hashCode()`（确定性）
- `deviceId` 仅在请求头提供 `X-Device-Id` 时参与 key 拼接，用于同租户多设备打散

### 白名单（`rolloutType=whitelist`）

- 命中规则：`tenantId`（trim 后）存在于白名单集合即命中
- 存储形式：数据库中用逗号分隔字符串存储；编码/解码会对租户 id 做 trim、去空、去重、排序（得到稳定表示）

**支持能力**

- 全量发布（all）
- 按比例发布（percent）
- 白名单发布（whitelist）
- 强制升级（isForced）
- 定时强制升级（forceAfterAt）

> 说明：当前服务端不会基于 `forceAfterAt` 判断“是否到点必须升级”，仅将其原样返回给客户端；由客户端自行决定 UI/拦截策略。

**原则**

- 灰度必须稳定：同一 tenant 不应反复进出灰度范围
- 版本不同要隔离：使用 `version/salt` 作为扰动项，避免不同版本共享灰度结果
- 灰度发布要可止损：支持暂停发布（`paused`）

## 安全机制（下载与完整性）

- **完整性校验**：客户端必须校验 `sha256` 与 `fileSize`
- **下载链接保护**：
  - 对象存储私有桶 + **预签名 URL**（短期有效）

## 演进方向

- 对象存储 presign：实现 `POST /api/releases/{id}/artifact:presign`，并支持仅存 `artifactKey`、查询时生成短期下载 URL
- 客户端检查接口增强：对 `artifactKey` 做兼容（避免 `artifactUrl` 未配置导致 500）
- 强更时点语义：明确 `forceAfterAt` 的服务端/客户端职责（是否由服务端在到点后强制返回 `isForced=true`）
- 管理端查询：为 `/api/releases` 增加分页与排序（当前为 `findAll` + 内存过滤）
- 统计上报：实现 `POST /api/client-updates/report` 与聚合看板