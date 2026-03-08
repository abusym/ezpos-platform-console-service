# 10 — Kotlin 标准库实用函数

## 从一行代码说起

项目中初始化管理员账号时有这么一行：

```kotlin
passwordHash = requireNotNull(passwordEncoder.encode("123456")) {
    "PasswordEncoder.encode returned null"
}
```

Go 程序员看到会懵——`requireNotNull` 哪来的？大括号又是什么？这一篇就把 Kotlin 标准库常用的工具函数讲清楚。

---

## 一、前置知识：Kotlin 的空安全

Go 里指针可能是 `nil`，但编译器不会强制你检查。Kotlin 直接在类型系统里区分了：

| Kotlin 类型 | 含义 | Go 类比 |
|-------------|------|---------|
| `String` | 绝对不为 null | `string`（值类型，不可能 nil） |
| `String?` | 可能为 null | `*string`（指针，可能 nil） |

编译器会**拒绝**你把 `String?` 当 `String` 用，必须先处理 null 的情况。

```kotlin
val name: String? = getName()
println(name.length)      // ❌ 编译错误！name 可能为 null
println(name?.length)     // ✅ 安全调用，name 为 null 时结果也是 null
println(name!!.length)    // ✅ 强制断言非 null，为 null 则抛异常（类似 Go 的野指针崩溃）
```

---

## 二、requireNotNull — 空值检查 + 类型收窄

### 函数签名

```kotlin
fun <T : Any> requireNotNull(value: T?, lazyMessage: () -> String): T
```

用 Go 伪代码理解：

```go
func RequireNotNull[T any](value *T, message func() string) T {
    if value == nil {
        panic(message())  // 抛 IllegalArgumentException
    }
    return *value
}
```

### 作用

1. **运行时检查**：值为 null 就崩溃，抛出 `IllegalArgumentException`
2. **类型收窄**：返回值从 `T?` 变成 `T`，后续代码不用再处理 null

### 回到那行代码

```kotlin
// passwordEncoder.encode() 来自 Java 接口，返回类型是 String?（Java 不保证非 null）
val encoded: String? = passwordEncoder.encode("123456")

// requireNotNull 把 String? 转成 String，null 时直接崩
val passwordHash: String = requireNotNull(encoded) {
    "PasswordEncoder.encode returned null"
}
```

### 大括号 `{ ... }` 是什么？

是 **lambda 表达式**（匿名函数），作为 `lazyMessage` 参数传入。只有值为 null 时才执行，避免不必要的字符串拼接开销。

Go 里没有这种语法，等价写法是：

```go
if encoded == nil {
    panic("PasswordEncoder.encode returned null")
}
```

Kotlin 的 **trailing lambda** 语法规则：当函数的最后一个参数是 lambda 时，可以把它写在圆括号外面。所以这两种写法等价：

```kotlin
// 写法 1：lambda 在括号内
requireNotNull(value, { "error message" })

// 写法 2：trailing lambda（更常用）
requireNotNull(value) { "error message" }
```

---

## 三、一族兄弟：前置条件检查函数

Kotlin 标准库提供了一组"断言式"函数，用于在函数入口做前置检查：

| 函数 | 失败时抛出 | 用途 | Go 类比 |
|------|-----------|------|---------|
| `require(condition)` | `IllegalArgumentException` | 检查参数合法性 | `if !cond { panic() }` |
| `requireNotNull(value)` | `IllegalArgumentException` | 参数不能为 null | `if val == nil { panic() }` |
| `check(condition)` | `IllegalStateException` | 检查对象状态 | `if !cond { panic() }` |
| `checkNotNull(value)` | `IllegalStateException` | 状态相关值不能为 null | `if val == nil { panic() }` |
| `error("msg")` | `IllegalStateException` | 直接抛异常 | `panic("msg")` |

### 使用示例

```kotlin
fun createUser(name: String, age: Int) {
    // 检查参数
    require(age > 0) { "age must be positive, got $age" }
    require(name.isNotBlank()) { "name must not be blank" }

    // 检查状态
    check(!isClosed) { "service is already closed" }

    // 确保非 null
    val config = checkNotNull(loadConfig()) { "config not loaded" }
}
```

Go 等价写法：

```go
func CreateUser(name string, age int) {
    if age <= 0 {
        panic(fmt.Sprintf("age must be positive, got %d", age))
    }
    if strings.TrimSpace(name) == "" {
        panic("name must not be blank")
    }
    if isClosed {
        panic("service is already closed")
    }
    config := loadConfig()
    if config == nil {
        panic("config not loaded")
    }
}
```

> **何时用 `require` vs `check`？**
> - `require` = 调用方的错（"你给我的参数不对"）
> - `check` = 自身的状态不对（"现在不该调这个方法"）
>
> 本质区别只是抛出的异常类型不同，方便上层按类型做不同处理。

---

## 四、作用域函数：let / run / apply / also / with

这是 Kotlin 标准库另一组高频函数，用于简化对象操作。

### 快速对照表

| 函数 | 引用对象方式 | 返回值 | 典型用途 |
|------|-------------|--------|---------|
| `let` | `it` | lambda 结果 | null 安全调用链 |
| `run` | `this` | lambda 结果 | 对象上执行并返回结果 |
| `apply` | `this` | 对象本身 | 对象初始化/配置 |
| `also` | `it` | 对象本身 | 附加操作（如打日志） |
| `with(obj)` | `this` | lambda 结果 | 对同一对象做多个操作 |

### 最常用场景

**`let` — null 安全链**（项目中最常见）

```kotlin
// Kotlin
val length = name?.let { it.trim().length } ?: 0

// 等价 Go
var length int
if name != nil {
    length = len(strings.TrimSpace(*name))
} else {
    length = 0
}
```

**`apply` — 对象初始化**

```kotlin
// Kotlin
val user = PlatformUser().apply {
    username = "admin"
    displayName = "Administrator"
    enabled = true
}

// 等价 Go
user := &PlatformUser{}
user.Username = "admin"
user.DisplayName = "Administrator"
user.Enabled = true
```

**`also` — 附加操作（不影响链式调用）**

```kotlin
// Kotlin
return repo.save(user).also {
    log.info("Created user: {}", it.username)
}

// 等价 Go
saved := repo.Save(user)
log.Info("Created user: %s", saved.Username)
return saved
```

---

## 五、其他常用标准库函数

### 集合操作

```kotlin
// Kotlin 的集合操作非常函数式，Go 1.21+ 的 slices 包开始有类似功能
val names = users.map { it.name }           // 提取字段 → []string
val admins = users.filter { it.isAdmin }    // 过滤
val found = users.find { it.id == 123 }     // 查找第一个匹配（返回 T?）
val exists = users.any { it.age > 18 }      // 是否存在匹配
val sorted = users.sortedBy { it.name }     // 排序
```

### 字符串模板

```kotlin
// Kotlin — 直接在字符串里嵌入变量/表达式
val msg = "User $username has ${orders.size} orders"

// Go
msg := fmt.Sprintf("User %s has %d orders", username, len(orders))
```

### Elvis 操作符 `?:`

```kotlin
// Kotlin — 左边为 null 时用右边的默认值
val name = user.nickname ?: "Anonymous"

// Go
name := user.Nickname
if name == "" {  // Go 没有 null，只能判零值
    name = "Anonymous"
}
```

---

## 小结

| 概念 | 一句话 |
|------|--------|
| `requireNotNull` | "这个值不能是 null，是的话就崩"——参数校验 |
| `checkNotNull` | 同上，但语义是状态校验 |
| `require` / `check` | 条件断言，false 就崩 |
| `?.let { }` | 安全地对可空值做操作 |
| `apply { }` | 配置/初始化对象 |
| `also { }` | 链式调用中插入副作用 |
| `?:` | 空值时给默认值 |

这些都是 **Kotlin 标准库自带的顶层函数**，不需要 import，任何地方直接用——就像 Go 里的 `len()`、`append()`、`make()` 一样是语言/标准库的一部分。
