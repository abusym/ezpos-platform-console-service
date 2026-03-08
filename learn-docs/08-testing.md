# 08 — 测试方法

## 测试框架对比

| 维度 | Go | Kotlin/Spring |
|------|-----|---------------|
| 测试框架 | `testing` 标准库 | **JUnit 5** |
| Mock 库 | testify/mock / gomock | **MockK** |
| 断言 | testify/assert | JUnit Assertions / kotlin.test |
| 测试文件 | `xxx_test.go`（同目录） | `src/test/kotlin/`（镜像目录） |
| 运行命令 | `go test ./...` | `./mvnw test` |

## 目录结构

```
src/main/kotlin/net/ezpos/console/     ← 源码
  feature/user/service/
    PlatformUserService.kt

src/test/kotlin/net/ezpos/console/     ← 测试（镜像结构）
  feature/user/service/
    PlatformUserServiceTest.kt
```

Go 的测试文件和源码在同一个目录，Spring 的测试在 `src/test/` 下的镜像目录。

## 单元测试模式

### Go 的测试

```go
func TestUserService_Create(t *testing.T) {
    // 创建 mock
    mockRepo := new(MockUserRepo)
    mockEncoder := new(MockPasswordEncoder)
    service := NewUserService(mockRepo, mockEncoder)

    // 设置 mock 行为
    mockRepo.On("ExistsByUsername", "newuser").Return(false)
    mockEncoder.On("Encode", "password").Return("hashed")
    mockRepo.On("Save", mock.Anything).Return(&User{ID: 1, Username: "newuser"}, nil)

    // 执行
    result, err := service.Create(&CreateUserRequest{
        Username: "newuser",
        Password: "password",
    })

    // 断言
    assert.NoError(t, err)
    assert.Equal(t, "newuser", result.Username)
    mockRepo.AssertExpectations(t)
}
```

### Kotlin 的测试（本项目风格）

```kotlin
class PlatformUserServiceTest {

    // 创建 mock 依赖
    private val repository = mockk<PlatformUserRepository>()
    private val mapper = mockk<PlatformUserMapper>()
    private val passwordEncoder = mockk<PasswordEncoder>()

    // 用 mock 构造被测试的 Service
    private val service = PlatformUserService(repository, mapper, passwordEncoder)

    @Test
    fun `create - 正常创建用户`() {
        // given（准备数据和 mock 行为）
        val request = CreatePlatformUserRequest(
            username = "newuser",
            password = "password123",
            displayName = "New User"
        )

        every { repository.existsByUsername("newuser") } returns false
        every { passwordEncoder.encode("password123") } returns "hashed_password"
        every { repository.save(any()) } answers {
            firstArg<PlatformUser>().apply { id = 1L }
        }
        every { mapper.toDto(any()) } returns PlatformUserDto(
            id = 1L,
            username = "newuser",
            displayName = "New User"
        )

        // when（执行被测方法）
        val result = service.create(request)

        // then（验证结果）
        assertEquals("newuser", result.username)
        assertEquals("New User", result.displayName)

        // 验证 mock 被正确调用
        verify { repository.existsByUsername("newuser") }
        verify { passwordEncoder.encode("password123") }
        verify { repository.save(any()) }
    }

    @Test
    fun `create - 用户名已存在时抛出异常`() {
        // given
        val request = CreatePlatformUserRequest(
            username = "existing",
            password = "password123"
        )
        every { repository.existsByUsername("existing") } returns true

        // when & then
        assertThrows<EntityAlreadyExistsException> {
            service.create(request)
        }
    }
}
```

## MockK 速查

MockK 是 Kotlin 专用的 Mock 库，类似 Go 的 testify/mock：

```kotlin
// 创建 mock
val repo = mockk<UserRepository>()

// 设置行为（类似 mockRepo.On("Method").Return(value)）
every { repo.findById(1L) } returns Optional.of(user)
every { repo.findById(999L) } returns Optional.empty()
every { repo.save(any()) } answers { firstArg() }  // 返回传入的参数
every { repo.existsByUsername("admin") } returns true

// 对于无返回值的方法
every { repo.deleteById(any()) } just Runs

// 验证调用（类似 mockRepo.AssertCalled(t, "Method", args)）
verify { repo.findById(1L) }
verify(exactly = 1) { repo.save(any()) }
verify(exactly = 0) { repo.deleteById(any()) }  // 确认没有被调用
```

### 对比 Go testify/mock

| 操作 | Go testify/mock | Kotlin MockK |
|------|----------------|--------------|
| 创建 mock | `mock.Mock` + 手写方法 | `mockk<Interface>()` |
| 设置返回值 | `.On("Method", arg).Return(val)` | `every { obj.method(arg) } returns val` |
| 匹配任意参数 | `mock.Anything` | `any()` |
| 验证调用 | `.AssertCalled(t, "Method")` | `verify { obj.method() }` |
| 验证调用次数 | `.AssertNumberOfCalls(t, "Method", 1)` | `verify(exactly = 1) { obj.method() }` |

MockK 的优势是**类型安全**——`every { repo.findById(1L) }` 如果参数类型不对，编译就会报错。Go 的 mock 是字符串匹配方法名，容易拼错。

## 测试命名风格

本项目使用 Kotlin 的反引号方法名，允许用中文/自然语言命名测试：

```kotlin
@Test
fun `create - 正常创建用户`() { ... }

@Test
fun `create - 用户名已存在时抛出 EntityAlreadyExistsException`() { ... }

@Test
fun `getById - 用户不存在时抛出 EntityNotFoundException`() { ... }

@Test
fun `login - 密码错误时抛出 AuthenticationFailedException`() { ... }
```

Go 的测试命名：

```go
func TestCreate_Success(t *testing.T) { ... }
func TestCreate_UsernameAlreadyExists(t *testing.T) { ... }
```

## 测试的核心理念：不需要 Spring 容器

本项目的单元测试**完全不启动 Spring**。每个测试类只需要：

1. 用 `mockk()` 创建假的依赖
2. 手动构造被测试的 Service
3. 设置 mock 行为
4. 调用方法并断言结果

```kotlin
// 不需要任何 Spring 注解！纯 Kotlin 测试
class MerchantServiceTest {
    private val repository = mockk<MerchantRepository>()
    private val mapper = mockk<MerchantMapper>()
    private val service = MerchantService(repository, mapper)

    // 测试就像调用普通函数一样简单
}
```

这和你在 Go 里写单元测试的思路完全一样——mock 依赖，测试业务逻辑，不涉及数据库和网络。

## 运行测试

```bash
# 运行所有测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ReleaseServiceTest

# 运行单个测试方法（需要方法名）
./mvnw test -Dtest="ReleaseServiceTest#create - 正常创建发布"
```

对比 Go：

```bash
go test ./...
go test ./feature/release/ -run TestCreate
```

## 一个完整的测试文件示例

```kotlin
class RolloutDeciderTest {

    private val decider = RolloutDecider()  // 无依赖，直接 new

    @Test
    fun `ALL rollout type - always returns true`() {
        val release = createRelease(rolloutType = ReleaseRolloutType.ALL)

        assertTrue(decider.isIncluded(release, "tenant-1", null))
        assertTrue(decider.isIncluded(release, "tenant-2", "device-1"))
    }

    @Test
    fun `WHITELIST rollout type - included tenant returns true`() {
        val release = createRelease(
            rolloutType = ReleaseRolloutType.WHITELIST,
            whitelistTenants = "tenant-1,tenant-2"
        )

        assertTrue(decider.isIncluded(release, "tenant-1", null))
        assertFalse(decider.isIncluded(release, "tenant-3", null))
    }

    @Test
    fun `PERCENT rollout type - deterministic bucketing`() {
        val release = createRelease(
            rolloutType = ReleaseRolloutType.PERCENT,
            percent = 50,
            rolloutSalt = "test-salt"
        )

        // 同一个 tenantId 多次调用结果一致（稳定分桶）
        val result1 = decider.isIncluded(release, "tenant-1", null)
        val result2 = decider.isIncluded(release, "tenant-1", null)
        assertEquals(result1, result2)
    }

    // 辅助方法：创建测试用的 Release 对象
    private fun createRelease(
        rolloutType: ReleaseRolloutType,
        percent: Int? = null,
        whitelistTenants: String? = null,
        rolloutSalt: String? = null
    ): Release {
        return Release(
            applicationCode = "app",
            platform = "android",
            version = "1.0.0",
            minSupportedVersion = "0.9.0",
            rolloutType = rolloutType,
            percent = percent,
            whitelistTenants = whitelistTenants,
            rolloutSalt = rolloutSalt,
            status = ReleaseStatus.PUBLISHED
        )
    }
}
```

这个测试类测的是 `RolloutDecider`（灰度发布决策器），它是一个纯计算类，没有任何依赖，所以测试非常简单直观。
