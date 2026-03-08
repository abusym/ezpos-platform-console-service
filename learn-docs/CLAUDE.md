# CLAUDE.md — learn-docs 工作指南

## 这个目录是什么

本目录 (`learn-docs/`) 是面向**有 Gin/GORM 经验的前端工程师**的 Spring Boot 学习文档集，基于父项目 `ezpos-platform-console-service` 的真实源码编写。

## 文档清单与阅读顺序

| 序号 | 文件 | 主题 |
|------|------|------|
| 00 | `00-overview.md` | 项目总览、技术栈对照、核心思维差异 |
| 01 | `01-project-structure.md` | Feature-first 分包、分层架构、模块依赖 |
| 02 | `02-spring-boot-basics.md` | Kotlin 语法速查、Spring Boot 核心概念、注解、Bean、Maven |
| 03 | `03-dependency-injection.md` | IoC 容器、构造函数注入、接口与实现、SPI 模式 |
| 04 | `04-database-and-orm.md` | JPA vs GORM：Entity、Repository、分页、事务、Flyway、MapStruct |
| 05 | `05-api-and-controllers.md` | Controller 注解式路由、参数绑定、校验、全部 API 一览 |
| 06 | `06-authentication.md` | Opaque Token + Redis 认证、Filter vs Gin 中间件 |
| 07 | `07-error-handling.md` | Go error vs Kotlin Exception、全局异常处理、ProblemDetail |
| 08 | `08-testing.md` | JUnit 5 + MockK、对比 Go testing/testify |
| 09 | `09-build-and-run.md` | 环境准备、常用命令、配置切换、问题排查 |
| 10 | `10-kotlin-stdlib-essentials.md` | Kotlin 标准库常用 API |

## 编写原则

- **目标读者**：熟悉 TypeScript/前端 + 用过 Go Gin/GORM 写过后端、Spring Boot 零基础
- **每个概念必须提供 Gin/GORM ↔ Spring Boot 的代码对比**
- **基于父项目真实源码举例**，不写脱离项目的泛泛示例
- 中文撰写，代码注释中英文均可
- 不使用 emoji

## 修改和扩展指南

- 新增文档按序号递增命名：`11-xxx.md`、`12-xxx.md`
- 新增后同步更新本文件的文档清单表格
- 如果父项目源码发生变化（新增模块、改变架构），需要同步更新对应文档中的代码示例
- 父项目源码位于 `../src/main/kotlin/net/ezpos/console/`，测试位于 `../src/test/kotlin/`
