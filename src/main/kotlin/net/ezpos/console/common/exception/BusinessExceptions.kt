package net.ezpos.console.common.exception

/**
 * 业务异常基类。
 *
 * 所有业务异常继承此类，由 [net.ezpos.console.common.web.problem.GlobalExceptionHandler] 统一映射为 HTTP 响应。
 */
abstract class BusinessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** 请求的实体不存在（→ 404）。 */
class EntityNotFoundException(
    val entity: String,
    val identifier: Any,
) : BusinessException("$entity not found: $identifier")

/** 创建实体时违反唯一约束（→ 409）。 */
class EntityAlreadyExistsException(message: String) : BusinessException(message)

/** 业务规则校验不通过，如版本号不合法、灰度配置错误等（→ 400）。 */
class BusinessRuleException(message: String) : BusinessException(message)

/** 认证失败：凭据错误或账号禁用（→ 401）。 */
class AuthenticationFailedException(message: String) : BusinessException(message)

/** 已存储数据不一致 / 损坏，属于服务端问题（→ 500）。 */
class DataIntegrityException(message: String) : BusinessException(message)
