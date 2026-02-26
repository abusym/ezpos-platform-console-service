# 代码包结构与功能模块划分（Package Structure）

本文档描述 `ezpos-platform-console-service` 的 **package 组织规范**：以“功能模块（feature module）”为一级维度，再在模块内按常见分层（controller/service/repository/entity/dto...）组织代码。

该规范的目标是：当平台能力逐步扩展到 `merchant`、`subscription`、`import`、`migration`、`release` 等多个业务域时，依然能保持代码可定位、可演进、可测试，并尽量降低跨模块耦合。

## 总体原则

- **按模块归档**：先找“模块”，再在模块内找“分层”。例如：`feature/merchant/service/*`。
- **模块内聚、模块间低耦合**：跨模块调用要克制，优先通过“应用服务（service）”暴露用例能力，而不是直接访问对方的 `repository/entity`。
- **横切能力下沉到 common**：例如 security、id 生成、通用异常、统一响应/错误码、审计、可观测性等，放到 `common/*`。
- **对外 API 与对内模型分离**：Controller 层使用 `dto`（request/response），持久化使用 `entity`，避免在 API 直接暴露 JPA entity。
- **命名与职责稳定**：`controller` 只做 HTTP 适配（校验/鉴权/调用用例/返回 DTO），业务规则进 `service`（或更细的 domain/service）。

## 推荐的根目录结构

以 `net.ezpos.console` 为根包：

- `net.ezpos.console.feature.*`：按业务模块组织代码（merchant/subscription/import/...）
- `net.ezpos.console.common.*`：通用能力（跨模块复用、与业务无关）
- `net.ezpos.console.infra.*`（可选）：更偏“基础设施适配”的实现（消息、对象存储、第三方支付、外部系统客户端等）

> 目前仓库里已经采用了 `feature/user/*` 这种“模块优先”的思路（例如 `feature/user/controller`、`feature/user/service`、`feature/user/repository` 等），本文档是在此基础上扩展到更多模块。

## 单个功能模块的推荐子包

下面以未来的 `merchant` 模块为例（路径只展示建议，不要求一次性全部具备）：

```
net/ezpos/console/feature/merchant/
  controller/        # REST API 适配层（Spring MVC）
  dto/               # Request/Response DTO（按用例拆分也可以）
  service/           # 应用服务（用例编排、事务边界、领域规则入口）
  entity/            # JPA Entity（持久化模型）
  repository/        # Spring Data Repository
  mapper/            # MapStruct Mapper 接口（DTO <-> Entity/Domain 映射）
  event/             # 领域事件/集成事件（如 outbox payload）
  job/               # 定时任务/异步任务的入口（如 Spring Scheduler）
  config/            # 模块自身配置（properties/bean/feature flag）
  security/          # 模块特有的鉴权辅助（如权限常量、method security 规则）
```

### 子包职责边界（建议）

- `controller`
  - 只负责：请求解析、参数校验、鉴权注解、调用 `service`、返回 DTO
  - 不建议：直接操作 `repository` 或写复杂业务逻辑
- `service`
  - 用例编排：比如“创建商家 + 初始化订阅 + 写审计日志”
  - 事务边界：一般由 service 层控制（`@Transactional`）
  - 对外暴露：尽量用清晰的“用例方法”，而不是泄漏 entity
- `entity` / `repository`
  - 仅对本模块内部直接可见（通过团队约定/代码评审约束）
  - 其他模块若需要数据，优先通过本模块的 service 获取
- `dto`
  - 按 API 用例拆分：`CreateMerchantRequest`、`MerchantDto`、`UpdateMerchantRequest`...
  - 与 entity 分离，避免把 JPA 注解/懒加载等细节带到 API
- `mapper`
  - 存放 MapStruct 相关的 Mapper 接口（以及必要的自定义转换）
  - 只负责模型转换，不承载业务规则/用例编排逻辑

## common 包建议拆分

当前仓库已有 `common/security`、`common/entity` 等基础能力，后续可按需要补充：

- `common/web`
  - 全局异常处理（`@ControllerAdvice`）
  - 统一错误响应结构（error code / trace id）
  - 分页/排序等通用模型
- `common/security`
  - 通用鉴权 filter、token SPI、principal 模型、权限表达式工具
- `common/entity`
  - `IdEntity`、Snowflake、审计字段基类（createdAt/createdBy 等）
- `common/observability`
  - trace/logging 相关（若引入 Micrometer/OTel）

## 跨模块依赖约定（强烈建议）

为了让模块数量增加时不失控，建议约定以下依赖方向：

- **允许**：`feature/<module>` 依赖 `common/*`
- **允许（谨慎）**：`feature/A/service` 调用 `feature/B/service` 的公开用例
- **避免**：跨模块直接引用对方的 `entity/repository`
- **避免**：一个模块的 controller 直接调用另一个模块的 repository

如果后续模块多且耦合提升，可以进一步引入：

- `feature/<module>/api`：对外公开的接口（或 SPI），由其他模块依赖
- `feature/<module>/internal`：内部实现（entity/repository）不被外部依赖（需要靠约定/构建规则保障）

## 迁移现有代码的建议路径

当前 `feature/user` 已经符合“模块优先 + 模块内分层”的结构。建议后续新增模块时：

- **直接照 `feature/user` 的组织方式复制骨架**
- **优先把横切能力放到 `common/*`**（避免每个模块重复实现）
- **先约定边界，再写代码**：跨模块只调 service，用例接口清晰化

