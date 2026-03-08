# 04 — JPA / Hibernate vs GORM

## 总体对比

| 特性 | GORM (Go) | JPA / Hibernate (Kotlin) |
|------|-----------|-------------------------|
| 模型定义 | struct + tag | class + 注解 |
| 查询方式 | 链式调用 `db.Where().Find()` | 方法命名约定 / JPQL / Specification |
| 迁移 | AutoMigrate / golang-migrate | Flyway（SQL 文件） |
| 关联 | `gorm:"foreignKey"` | `@ManyToOne` / `@OneToMany` |
| 事务 | `db.Transaction(func(tx) {...})` | `@Transactional` 注解 |
| 连接管理 | 手动 `gorm.Open()` | 自动配置（application.yaml） |

## Entity（实体）= GORM 的 Model

### GORM 写法

```go
type User struct {
    gorm.Model                     // 内嵌 ID, CreatedAt, UpdatedAt, DeletedAt
    Username    string `gorm:"uniqueIndex;not null"`
    Password    string `gorm:"not null"`
    DisplayName string
    Email       string
    Enabled     bool   `gorm:"default:true"`
}
```

### JPA 写法（本项目 `PlatformUser.kt`）

```kotlin
@Entity                                    // "我是一个数据库表"
@Table(name = "platform_users")            // 表名
class PlatformUser(
    var username: String,                  // 列名自动映射（username → username）
    var passwordHash: String,              // 自动映射（passwordHash → password_hash）
    var displayName: String?,              // ? 表示可以为 null
    var email: String?,
    var enabled: Boolean = true,

    @UpdateTimestamp                        // 更新时自动设置时间
    var updatedAt: OffsetDateTime? = null
) : IdEntity()                             // 继承基类，获得 id + createdAt
```

**关键差异：**

1. **命名策略**：JPA 自动将 `camelCase` 转为 `snake_case`。`passwordHash` → `password_hash` 列。GORM 也这样做。
2. **可空类型**：Kotlin 用 `?` 表示可以为 null（`String?`），和 Go 的 `*string` 概念一样。
3. **继承**：所有 Entity 继承 `IdEntity`（提供 id + createdAt），相当于 GORM 的 `gorm.Model`。

## Repository（仓库）= GORM 的查询

这是 JPA 最"神奇"的部分。你只需要**定义接口方法名**，JPA 自动生成 SQL 查询！

### GORM 查询

```go
// Go: 你需要手写查询逻辑
func (r *UserRepo) FindByUsername(username string) (*User, error) {
    var user User
    result := r.db.Where("username = ?", username).First(&user)
    if result.Error != nil {
        return nil, result.Error
    }
    return &user, nil
}

func (r *UserRepo) ExistsByUsername(username string) bool {
    var count int64
    r.db.Model(&User{}).Where("username = ?", username).Count(&count)
    return count > 0
}
```

### JPA Repository

```kotlin
// Kotlin: 只定义方法名，JPA 自动实现！
interface PlatformUserRepository : JpaRepository<PlatformUser, Long> {
    fun findByUsername(username: String): PlatformUser?
    fun existsByUsername(username: String): Boolean
}
```

**是的，就这么简单。** 你不需要写任何实现代码！

JPA 看到方法名 `findByUsername`，自动理解为：
- `find` → SELECT 查询
- `By` → WHERE 条件
- `Username` → username 列

它会自动生成 `SELECT * FROM platform_users WHERE username = ?` 并执行。

### 方法命名规则速查

| 方法名 | 生成的 SQL |
|--------|-----------|
| `findByUsername(name)` | `WHERE username = ?` |
| `findByEnabledTrue()` | `WHERE enabled = true` |
| `existsByUsername(name)` | `SELECT count(*) > 0 WHERE username = ?` |
| `findByStatusAndPlatform(s, p)` | `WHERE status = ? AND platform = ?` |
| `findByEndDateBefore(date)` | `WHERE end_date < ?` |
| `countByStatus(status)` | `SELECT count(*) WHERE status = ?` |
| `deleteByUsername(name)` | `DELETE WHERE username = ?` |

### JpaRepository 自带方法

`JpaRepository<Entity, IdType>` 继承后，你**免费获得**这些方法：

```kotlin
repository.save(entity)           // INSERT 或 UPDATE（有 ID 就 UPDATE）
repository.findById(id)           // SELECT * WHERE id = ?
repository.findAll()              // SELECT * （全部）
repository.findAll(pageable)      // SELECT * LIMIT ? OFFSET ?（分页）
repository.deleteById(id)         // DELETE WHERE id = ?
repository.count()                // SELECT count(*)
repository.existsById(id)         // SELECT count(*) > 0 WHERE id = ?
```

对比 GORM：

```go
db.Create(&user)       // ≈ repository.save(entity)
db.First(&user, id)    // ≈ repository.findById(id)
db.Find(&users)        // ≈ repository.findAll()
db.Delete(&user, id)   // ≈ repository.deleteById(id)
```

## 复杂查询：Specification

当方法名搞不定复杂查询时，JPA 提供 `JpaSpecificationExecutor`，本项目的 `ReleaseRepository` 就用了：

```kotlin
interface ReleaseRepository : JpaRepository<Release, Long>,
                               JpaSpecificationExecutor<Release> {
    // JpaSpecificationExecutor 让你可以用 Specification 做复杂查询
}
```

```kotlin
// ReleaseSpecifications.kt — 构建动态查询条件
object ReleaseSpecifications {
    fun withFilters(
        applicationCode: String?,
        platform: String?,
        status: ReleaseStatus?
    ): Specification<Release> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            applicationCode?.let {
                predicates += cb.equal(root.get<String>("applicationCode"), it)
            }
            platform?.let {
                predicates += cb.equal(root.get<String>("platform"), it)
            }
            status?.let {
                predicates += cb.equal(root.get<ReleaseStatus>("status"), it)
            }
            cb.and(*predicates.toTypedArray())
        }
    }
}
```

类比 GORM 的动态查询：

```go
// Go: GORM 动态查询
query := db.Model(&Release{})
if appCode != "" {
    query = query.Where("application_code = ?", appCode)
}
if platform != "" {
    query = query.Where("platform = ?", platform)
}
if status != "" {
    query = query.Where("status = ?", status)
}
query.Find(&releases)
```

思路完全一样，只是写法不同。

## 分页

### GORM 分页

```go
var users []User
db.Offset((page-1) * pageSize).Limit(pageSize).Find(&users)

var total int64
db.Model(&User{}).Count(&total)
```

### JPA 分页

```kotlin
// Controller
@GetMapping
fun list(pageable: Pageable): Page<PlatformUserDto> {
    return service.list(pageable)
}

// Service
fun list(pageable: Pageable): Page<PlatformUserDto> {
    return repository.findAll(pageable).map { mapper.toDto(it) }
}
```

Spring 自动从请求参数 `?page=0&size=20&sort=createdAt,desc` 解析出 `Pageable` 对象。

返回的 `Page` 对象包含：

```json
{
  "content": [...],          // 数据列表
  "totalElements": 100,      // 总记录数
  "totalPages": 5,           // 总页数
  "number": 0,               // 当前页码（从 0 开始）
  "size": 20                 // 每页大小
}
```

你不需要手动写 `OFFSET`、`LIMIT` 和 `COUNT` 查询！

## 事务管理

### GORM 事务

```go
err := db.Transaction(func(tx *gorm.DB) error {
    if err := tx.Create(&order).Error; err != nil {
        return err  // 返回 error 会自动回滚
    }
    if err := tx.Create(&payment).Error; err != nil {
        return err
    }
    return nil  // 返回 nil 自动提交
})
```

### JPA 事务

```kotlin
@Transactional  // 加上这个注解，整个方法就是一个事务
fun createSubscription(request: CreateSubscriptionRequest): SubscriptionDto {
    val merchant = merchantService.getById(request.merchantId)
    val plan = planService.getById(request.planId)

    val subscription = Subscription(
        merchantId = merchant.id,
        planId = plan.id,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(plan.durationDays.toLong()),
        status = SubscriptionStatus.ACTIVE
    )

    return mapper.toDto(repository.save(subscription))
    // 如果中间抛出异常，自动回滚
    // 如果正常返回，自动提交
}
```

**`@Transactional` 注解 = GORM 的 `db.Transaction()` 包装**，但更简洁——你不需要手动 `return error` 来触发回滚，抛出异常就自动回滚。

## 数据库迁移：Flyway

### golang-migrate 方式

```
migrations/
├── 000001_init.up.sql
├── 000001_init.down.sql
├── 000002_add_orders.up.sql
└── 000002_add_orders.down.sql
```

### Flyway 方式（本项目）

```
db/migration/
├── V1__init_schema.sql               # V1: 初始表
├── V2__add_client_update_reports.sql  # V2: 客户端更新报告表
├── V3__add_merchants.sql             # V3: 商户表
├── V4__add_plans_and_subscriptions.sql # V4: 计划和订阅表
└── V5__add_data_migrations_and_audit_logs.sql  # V5: 迁移和审计表
```

**命名规则**：`V{版本号}__{描述}.sql`（注意是两个下划线 `__`）

**Flyway 会在应用启动时自动执行未运行过的迁移**——它在数据库里维护一张 `flyway_schema_history` 表来记录哪些迁移已执行。

**GORM AutoMigrate vs Flyway：**
- GORM AutoMigrate 通过 Go 代码的 struct 定义来修改表结构（方便但危险）
- Flyway 通过手写 SQL 来管理表结构（更可控、更安全、适合团队协作）

## 雪花 ID（Snowflake ID）

本项目不用数据库自增 ID，而用雪花算法生成分布式唯一 ID：

```kotlin
@Id
@SnowflakeId  // 自定义注解，触发 SnowflakeIdGenerator
var id: Long? = null
```

`SnowflakeIdGenerator` 基于时间戳 + 机器 ID + 序列号生成 64 位唯一 ID。

**为什么用雪花 ID？**
- 分布式环境下不会冲突（不像自增 ID 需要协调）
- ID 天然按时间排序
- 适合 SaaS 多实例部署

## DTO 和 Mapper

### 为什么不直接返回 Entity？

```kotlin
// Entity 有敏感字段
class PlatformUser(
    var username: String,
    var passwordHash: String,  // 密码！不能返回给前端！
    ...
)

// DTO 只暴露安全的字段
data class PlatformUserDto(
    val id: Long,
    val username: String,
    // 没有 passwordHash
    val displayName: String?,
    val email: String?
)
```

### MapStruct 自动转换

```kotlin
@Mapper(componentModel = "spring")
interface PlatformUserMapper {
    fun toDto(entity: PlatformUser): PlatformUserDto
}
```

MapStruct 在**编译时**自动生成转换代码——它看到两个类有相同名字的字段，就自动赋值。

你在 Go 里可能手动写：

```go
func ToUserDTO(user *model.User) *dto.UserDTO {
    return &dto.UserDTO{
        ID:       user.ID,
        Username: user.Username,
        Email:    user.Email,
    }
}
```

MapStruct 帮你自动生成类似的代码，零运行时开销。
