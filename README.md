# Ezpos 平台总后台（Console Service）项目概览

本项目（`ezpos-platform-console-service`）是 **Ezpos SaaS 收银产品** 的“平台总后台/控制台”后端服务，面向平台运营与交付团队，用于统一管理所有商家（Merchant）及其生命周期相关能力。

## 背景与定位

- **Ezpos（主产品）**：面向商家的 SaaS 收银产品，承载门店侧业务闭环。
- **Console Service（本项目）**：面向平台方的总后台服务，提供平台运营、商家治理、数据统计、版本控制等平台能力，并通过 API/任务编排与 `ezpos` 主产品及其数据进行联动。

## 典型使用场景（平台侧）

- **商家管理**：商家入驻、资料维护、启停用、商家分组与标记等。
- **续费与订阅治理**：套餐（Plan）配置、订阅（Subscription）周期、续费、到期提醒、欠费处理等。
- **商品/业务数据迁移**：跨商家/跨环境的数据迁移（如商品、类目、会员等）。
- **客户端版本控制**：POS/管理端 App 版本发布、灰度/分组策略、强更控制。
- **平台运维**：统一的审计日志、操作留痕、任务中心、告警与健康检查等。

## 技术栈与关键依赖

- **语言/构建**：Kotlin 2.2、Maven Wrapper（`mvnw`/`mvnw.cmd`）
- **运行时**：Java 24
- **Web**：Spring Boot 4（Spring MVC）
- **数据**：Spring Data JPA + PostgreSQL
- **缓存/会话**：Redis（用于 opaque token）
- **安全**：Spring Security（无状态，Filter 鉴权）
- **API 文档**：springdoc-openapi（WebMVC UI）

## 配置与环境（Profiles）

### 公共配置

`src/main/resources/application.yaml` 定义了公共配置，并默认激活 `dev`：

- `spring.profiles.active: dev`
- Redis：`127.0.0.1:6379`（默认 DB 0）
- Opaque Token：
  - `ezpos.security.opaque-token.access-token-ttl`（默认 `24h`，可通过环境变量 `EZPOS_OPAQUE_TOKEN_TTL` 覆盖；当前实现为“滑动过期”，鉴权成功会自动续期）
  - `ezpos.security.opaque-token.redis-key-prefix`（默认 `ezpos:platform:opaque-token:`）
- Snowflake：
  - `snowflake.machine-id`（默认 `1`，环境变量 `EZPOS_SNOWFLAKE_MACHINE_ID`）
  - `snowflake.epoch`（默认 `1735689600000`，环境变量 `EZPOS_SNOWFLAKE_EPOCH`）

### 开发环境

开发环境建议在 `src/main/resources/application-dev.yaml` 中配置本地依赖（数据库等）。

- PostgreSQL：`jdbc:postgresql://localhost:5432/ezpos`
- 用户名/密码：`postgres` / `123456`
- JPA：`ddl-auto: update`

示例 `application-dev.yaml`（可直接复制）：

```yaml
# 开发环境配置
# 继承 application.yaml 的公共配置，这里只配置开发环境特有的部分
logging:
  level:
    org.hibernate.SQL: WARN

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ezpos
    username: postgres
    password: 123456

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: update
```

说明：

- 该项目在 `src/main/resources/application.yaml` 中默认激活 `dev`（`spring.profiles.active: dev`），所以通常只要放好 `application-dev.yaml` 就能生效。
- 如果你通过环境变量/启动参数覆盖了 profile（例如 `SPRING_PROFILES_ACTIVE=staging`），则 `application-dev.yaml` 不会生效，此时应改用对应的 `application-<profile>.yaml`。

## 本地启动（开发）

### 依赖准备

- PostgreSQL（创建数据库：`ezpos`）
- Redis（默认端口 `6379`）
- Java 24

### 启动命令

Windows（PowerShell/CMD）：

- `.\mvnw.cmd spring-boot:run`

或打包后运行：

- `.\mvnw.cmd -DskipTests package`
- `java -jar target\ezpos-saas-console-0.0.1-SNAPSHOT.jar`

### 验证

- 访问 Swagger UI：`/swagger-ui/index.html`
- 使用默认管理员（开发环境）登录：`admin / 123456`
- 将返回的 `accessToken` 作为 Bearer token 调用受保护接口

## 目录结构（当前）

- `src/main/kotlin/net/ezpos/console/feature/auth/*`：认证与 token 发行
- `src/main/kotlin/net/ezpos/console/feature/user/*`：平台用户（Platform User）与管理接口
- `src/main/kotlin/net/ezpos/console/common/security/*`：安全过滤器、SPI 与安全模型
- `src/main/kotlin/net/ezpos/console/common/entity/*`：ID/Snowflake 等基础设施

## 相关文档

- 代码包结构与模块划分建议：[`docs/architecture/package-structure.md`](docs/architecture/package-structure.md)
- 认证与鉴权说明：[`docs/auth/README.md`](docs/auth/README.md)
- Release（版本发布）模块设计：[`docs/modules/release/README.md`](docs/modules/release/README.md)
- Web 错误响应（ProblemDetail）说明：[`docs/web/README.md`](docs/web/README.md)

