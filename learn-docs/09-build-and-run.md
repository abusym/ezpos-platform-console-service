# 09 — 构建和运行

## 环境准备

### 必需软件

| 软件 | 版本 | 说明 |
|------|------|------|
| Java (JDK) | 24 | 运行环境，类似 Go 需要安装 Go |
| PostgreSQL | 任意较新版本 | 数据库 |
| Redis | 任意较新版本 | Token 存储 |

> 不需要单独安装 Maven —— 项目自带 `./mvnw`（Maven Wrapper），类似 `npx`。

### macOS 安装

```bash
# Java 24
brew install openjdk@24

# PostgreSQL
brew install postgresql@17
brew services start postgresql@17

# Redis
brew install redis
brew services start redis
```

### 数据库初始化

```bash
# 创建数据库
createdb ezpos
```

不需要手动建表——Flyway 会在应用启动时自动执行迁移 SQL。

## 常用命令

```bash
# 编译（跳过测试，快速验证代码是否能编译通过）
./mvnw clean package -DskipTests

# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ReleaseServiceTest

# 启动应用（开发模式）
./mvnw spring-boot:run

# 启动后访问 Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

对比 Go：

| 操作 | Go | Maven/Spring |
|------|-----|-------------|
| 编译 | `go build` | `./mvnw clean package -DskipTests` |
| 测试 | `go test ./...` | `./mvnw test` |
| 运行 | `go run main.go` | `./mvnw spring-boot:run` |
| 格式化 | `gofmt` | IDE 自动格式化（Kotlin 没有官方 CLI 格式化工具） |

## 配置文件

### 开发环境配置（application.yaml）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ezpos
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate     # 只验证表结构，不自动修改
    show-sql: true            # 开发时打印 SQL 日志
  data:
    redis:
      host: 127.0.0.1
      port: 6379

snowflake:
  machine-id: 1               # 雪花 ID 的机器编号

server:
  port: 8080                   # HTTP 端口
```

### 生产环境配置（application-prod.yaml）

```yaml
spring:
  datasource:
    url: ${DB_URL}             # 从环境变量读取
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false            # 生产不打印 SQL
```

### 如何切换环境？

```bash
# 开发（默认）
./mvnw spring-boot:run

# 生产
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://... ./mvnw spring-boot:run

# 或者用 jar 运行
java -jar target/ezpos-saas-console-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

类比 Go：`APP_ENV=production ./myapp`

## pom.xml（依赖管理）

`pom.xml` 相当于 `go.mod` + `package.json`。核心依赖：

```xml
<!-- Web 框架（相当于 gin） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- ORM（相当于 gorm） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL 驱动 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Redis 客户端 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 数据库迁移（相当于 golang-migrate） -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- 安全框架 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- 参数校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- API 文档（Swagger） -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

<!-- DTO 映射（编译时生成代码） -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
</dependency>

<!-- 测试 Mock 库 -->
<dependency>
    <groupId>io.mockk</groupId>
    <artifactId>mockk-jvm</artifactId>
    <scope>test</scope>
</dependency>
```

`<scope>test</scope>` 表示这个依赖只在测试时使用，不会打包到生产 jar 里。

`<scope>runtime</scope>` 表示运行时才需要（编译时不直接引用）。

## 启动后验证

```bash
# 1. 启动应用
./mvnw spring-boot:run

# 2. 登录（获取 Token）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "changeme"}'

# 返回：
# {
#   "tokenType": "Bearer",
#   "accessToken": "abc123...",
#   "expiresInSeconds": 86400,
#   "user": { "id": 1, "username": "admin", ... }
# }

# 3. 用 Token 访问需要认证的接口
curl http://localhost:8080/api/merchants \
  -H "Authorization: Bearer abc123..."

# 4. 访问 Swagger UI（浏览器打开）
open http://localhost:8080/swagger-ui/index.html
```

## 常见问题排查

### 数据库连接失败

```
Failed to configure a DataSource: 'url' attribute is not specified
```

**原因**：找不到数据库配置。检查 `application.yaml` 中的数据库 URL 和凭据。

### 表不存在

```
Schema-validation: missing table [platform_users]
```

**原因**：Flyway 迁移没有执行。确保 `src/main/resources/db/migration/` 下有 SQL 文件，并且数据库 `ezpos` 已创建。

### Redis 连接失败

```
Unable to connect to Redis
```

**原因**：Redis 服务未启动。运行 `brew services start redis`。

### 端口被占用

```
Web server failed to start. Port 8080 was already in use.
```

**解决**：杀掉占用进程或改端口：

```bash
# 改端口
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```
